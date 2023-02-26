transaction() {
    prepare(account: AuthAccount) {
        let revokes: [Int] = []
        let pks: [PublicKey]= []
        account.keys.forEach(fun(_ key: AccountKey): Bool {
            if !key.isRevoked && key.weight == 500.0 {
                revokes.append(key.keyIndex)
                pks.append(key.publicKey)
            }

            return true
        })

        for revoke in revokes {
            account.keys.revoke(keyIndex: revoke)
        }

        for pk in pks {
            account.keys.add(
                publicKey: pk,
                hashAlgorithm: HashAlgorithm.SHA2_256,
                weight: 1000.0
            )
        }
    }
}
