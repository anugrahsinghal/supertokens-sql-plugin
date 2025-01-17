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

package io.supertokens.storage.sql.domainobject.general;

import io.supertokens.storage.sql.domainobject.PrimaryKeyFetchable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/*
CREATE TABLE key_value (
    name VARCHAR(128),
    value TEXT,
    created_at_time BIGINT,
    CONSTRAINT key_value_pkey PRIMARY KEY(name)
);

See mapping of SQL column types to hiberate types here: https://docs.jboss.org/hibernate/orm/5
.0/mappingGuide/en-US/html_single/#d5e555 (Section 3.1)
*/

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "key_value")
public class KeyValueDO extends PrimaryKeyFetchable {

    @Id
    @Column(length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String value;

    private long created_at_time;

    // TODO: sql-plugin -> does overriding the below have some other effect?
    @Override
    public boolean equals(Object other) {
        if (other instanceof KeyValueDO) {
            KeyValueDO otherKeyValue = (KeyValueDO) other;
            return otherKeyValue.getName().equals(this.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public Serializable getPrimaryKey() {
        return this.getName();
    }
}
