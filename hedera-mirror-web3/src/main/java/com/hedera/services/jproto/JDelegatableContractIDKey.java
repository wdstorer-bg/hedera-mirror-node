/*
 * Copyright (C) 2020-2023 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.services.jproto;

import com.hederahashgraph.api.proto.java.ContractID;

/** Maps to proto Key of type contractID. */
public class JDelegatableContractIDKey extends JContractIDKey {
    public JDelegatableContractIDKey(final ContractID contractID) {
        super(contractID);
    }

    @Override
    public JDelegatableContractIDKey getDelegatableContractIdKey() {
        return this;
    }

    @Override
    public boolean hasDelegatableContractId() {
        return true;
    }

    @Override
    public boolean hasContractID() {
        return false;
    }

    @Override
    public JContractIDKey getContractIDKey() {
        return null;
    }

    @Override
    public String toString() {
        return "<JDelegatableContractId: " + getShardNum() + "." + getRealmNum() + "." + getContractID() + ">";
    }
}
