/*
 *    Copyright (c) 2022, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.storage.sql.domainobject.jwtsigning;

import io.supertokens.storage.sql.domainobject.PrimaryKeyFetchable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

//CREATE TABLE IF NOT EXISTS jwt_signing_keys (key_id VARCHAR(255) NOT NULL,
//key_string TEXT NOT NULL,algorithm VARCHAR(10) NOT NULL,
//created_at BIGINT,CONSTRAINT jwt_signing_keys_pkey PRIMARY KEY(key_id));

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jwt_signing_keys")
public class JWTSigningDO extends PrimaryKeyFetchable {

    @Id
    @Column(length = 255, nullable = false)
    private String key_id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String key_string;

    @Column(length = 10, nullable = false)
    private String algorithm;

    @Column(nullable = false)
    private long created_at;

    @Override
    public Serializable getPrimaryKey() {
        return getKey_id();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof JWTSigningDO) {
            JWTSigningDO otherKeyValue = (JWTSigningDO) other;
            return otherKeyValue.getKey_id().equals(this.getKey_id());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getKey_id().hashCode();
    }
}
