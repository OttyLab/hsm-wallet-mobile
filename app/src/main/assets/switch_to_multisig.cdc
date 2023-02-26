transaction() {
    prepare(account: AuthAccount) {
        let revokes: [Int] = []
        let pks: [PublicKey]= []
        account.keys.forEach(fun(_ key: AccountKey): Bool {
            if !key.isRevoked && key.weight == 1000.0 {
                revokes.append(key.keyIndex)
                pks.append(key.publicKey)
            }

            return true
        })

        if revokes.length < 2 {
            panic("At least 2 keys are required")
        }

        for revoke in revokes {
            account.keys.revoke(keyIndex: revoke)
        }

        for pk in pks {
            account.keys.add(
                publicKey: pk,
                hashAlgorithm: HashAlgorithm.SHA2_256,
                weight: 500.0
            )
        }
    }
}
