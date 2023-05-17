package peergos.shared.login;

import peergos.shared.cbor.*;
import peergos.shared.login.mfa.*;
import peergos.shared.user.*;
import peergos.shared.util.*;

import java.util.*;
import java.util.stream.*;

public class LoginResponse implements Cborable {

    public final Either<UserStaticData, List<MultiFactorAuthMethod>> resp;

    public LoginResponse(Either<UserStaticData, List<MultiFactorAuthMethod>> resp) {
        this.resp = resp;
    }

    @Override
    public CborObject toCbor() {
        SortedMap<String, Cborable> state = new TreeMap<>();
        state.put("a", new CborObject.CborBoolean(resp.isA()));
        state.put("r", resp.map(Cborable::toCbor,
                methods -> new CborObject.CborList(methods.stream().map(Cborable::toCbor).collect(Collectors.toList()))));
        return CborObject.CborMap.build(state);
    }

    public static LoginResponse fromCbor(Cborable cbor) {
        if (!(cbor instanceof CborObject.CborMap))
            throw new IllegalStateException("Invalid cbor for LoginResponse! " + cbor);
        CborObject.CborMap m = (CborObject.CborMap) cbor;
        boolean isA = m.getBoolean("a");
        if (isA)
            return new LoginResponse(Either.a(m.get("r", UserStaticData::fromCbor)));
        return new LoginResponse(Either.b(m.get("r", list -> ((CborObject.CborList)list).map(MultiFactorAuthMethod::fromCbor))));
    }
}
