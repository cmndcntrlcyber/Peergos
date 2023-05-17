package peergos.server.login;

import peergos.server.util.*;
import peergos.shared.corenode.*;
import peergos.shared.crypto.asymmetric.*;
import peergos.shared.crypto.hash.*;
import peergos.shared.login.mfa.*;
import peergos.shared.storage.*;
import peergos.shared.user.*;
import peergos.shared.util.*;

import java.util.*;
import java.util.concurrent.*;

public class VerifyingAccount implements Account {

    private final Account target;
    private final CoreNode core;
    private final ContentAddressedStorage storage;

    public VerifyingAccount(Account target, CoreNode core, ContentAddressedStorage storage) {
        this.target = target;
        this.core = core;
        this.storage = storage;
    }

    @Override
    public CompletableFuture<Boolean> setLoginData(LoginData login, byte[] auth) {
        PublicKeyHash identityHash = core.getPublicKeyHash(login.username).join().get();
        PublicSigningKey identity = storage.getSigningKey(identityHash).join().get();
        identity.unsignMessage(ArrayOps.concat(auth, login.serialize()));
        return target.setLoginData(login, auth);
    }

    @Override
    public CompletableFuture<Either<UserStaticData, List<MultiFactorAuthMethod>>> getLoginData(String username,
                                                                                               PublicSigningKey authorisedReader,
                                                                                               byte[] auth,
                                                                                               Optional<MultiFactorAuthResponse>  mfa) {
        return target.getLoginData(username, authorisedReader, auth, mfa).thenApply(res -> {
            TimeLimited.isAllowedTime(auth, 24*3600, authorisedReader);
            return res;
        });
    }

    @Override
    public CompletableFuture<List<MultiFactorAuthMethod>> getSecondAuthMethods(String username, byte[] auth) {
        PublicKeyHash identityHash = core.getPublicKeyHash(username).join().get();
        TimeLimited.isAllowed(Constants.LOGIN_URL + "listMfa", auth, 24*3600, storage, identityHash);
        return target.getSecondAuthMethods(username, auth);
    }

    @Override
    public CompletableFuture<Boolean> enableTotpFactor(String username, String uid, String code) {
        return target.enableTotpFactor(username, uid, code);
    }

    @Override
    public CompletableFuture<Boolean> deleteSecondFactor(String username, String uid, byte[] auth) {
        throw new IllegalStateException("TODO");
    }

    @Override
    public CompletableFuture<TotpKey> addTotpFactor(String username, byte[] auth) {
        PublicKeyHash identityHash = core.getPublicKeyHash(username).join().get();
        TimeLimited.isAllowed(Constants.LOGIN_URL + "addTotp", auth, 24*3600, storage, identityHash);
        return target.addTotpFactor(username, auth);
    }
}
