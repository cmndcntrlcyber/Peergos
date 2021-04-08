package peergos.shared.messaging;

import peergos.shared.cbor.*;

import java.util.*;
import java.util.stream.*;

/** A generalization of a vector clock that allows changing group membership
 *
 */
public class TreeClock implements Cborable {

    public final SortedMap<Id, Long> time;

    public TreeClock(SortedMap<Id, Long> time) {
        this.time = time;
    }

    public TreeClock merge(TreeClock other) {
        TreeMap<Id, Long> res = new TreeMap<>(time);
        for (Map.Entry<Id, Long> e : other.time.entrySet()) {
            if (! res.containsKey(e.getKey()) || res.get(e.getKey()) < e.getValue())
                res.put(e.getKey(), e.getValue());
        }
        return new TreeClock(res);
    }

    public boolean isBeforeOrEqual(TreeClock b) {
        for (Map.Entry<Id, Long> e : time.entrySet()) {
            if (!b.hasId(e.getKey()) || e.getValue() > b.getEventCounter(e.getKey()))
                return false;
        }
        return true;
    }

    public TreeClock removeMember(Id remover, Id toRemove) {
        TreeMap<Id, Long> res = new TreeMap<>(time);
        res.remove(toRemove);
        res.put(remover, res.get(remover) + 1);
        return new TreeClock(res);
    }

    public Set<Id> newMembersFrom(TreeClock other) {
        HashSet<Id> ids = new HashSet<>(other.time.keySet());
        ids.removeAll(time.keySet());
        return ids;
    }

    public TreeClock withMember(Id member) {
        TreeMap<Id, Long> res = new TreeMap<>(time);
        res.put(member, 0L);
        return new TreeClock(res);
    }

    public boolean hasId(Id member) {
        return time.containsKey(member);
    }

    public long getEventCounter(Id member) {
        Long res = time.get(member);
        if (res == null)
            throw new IllegalStateException("Id not present in clock!");
        return res;
    }

    public TreeClock increment(Id member) {
        Long counter = time.get(member);
        TreeMap<Id, Long> res = new TreeMap<>(time);
        res.put(member, counter + 1);
        return new TreeClock(res);
    }

    public static TreeClock init(List<Id> members) {
        TreeMap<Id, Long> res = new TreeMap<>();
        for (Id member : members) {
            res.put(member, 0L);
        }
        return new TreeClock(res);
    }

    @Override
    public CborObject toCbor() {
        List<List<Long>> res = new ArrayList<>();
        for (Map.Entry<Id, Long> e : time.entrySet()) {
            List<Long> mapping = Stream.concat(Arrays.stream(e.getKey().id).mapToObj(i -> (long) i), Stream.of(e.getValue()))
                    .collect(Collectors.toList());
            res.add(mapping);
        }
        return CborObject.CborList.build(res, m -> CborObject.CborList.build(m, CborObject.CborLong::new));
    }

    public static TreeClock fromCbor(Cborable cbor) {
        if (! (cbor instanceof CborObject.CborList))
            throw new IllegalStateException("Incorrect cbor: " + cbor);
        SortedMap<Id, Long> mappings = new TreeMap<>();
        ((CborObject.CborList) cbor)
                .map(m -> ((CborObject.CborList)m).map(i -> ((CborObject.CborLong)i).value))
                .forEach(m -> mappings.put(
                        new Id(m.subList(0, m.size() - 1).stream().mapToInt(Long::intValue).toArray()),
                        m.get(m.size() - 1)));
        return new TreeClock(mappings);
    }

    @Override
    public String toString() {
        return time.entrySet().stream()
                .map(p -> p.getKey() + ":" + p.getValue())
                .collect(Collectors.joining(","));
    }
}
