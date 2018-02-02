/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.id.RAExtDateId;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.MidPointIdProvidingSingleTableEntityPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * @author lazyman
 */
@Entity
//@IdClass(RAExtDateId.class)
@Table(name = "m_assignment_ext_date",
        indexes = {@Index(name = "iAExtensionDate", columnList= "item_id, dateValue")})
@Persister(impl = MidPointIdProvidingSingleTableEntityPersister.class)
public class RAExtDate extends RAExtBase<Timestamp> implements RAExtValue<Timestamp> {

    private Timestamp value;

    public RAExtDate() {
    }

    public RAExtDate(Timestamp value) {
        this.value = value;
    }

    @Id
    //@MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(value = {
            @JoinColumn(name = "anyContainer_owner_owner_oid", referencedColumnName = "owner_owner_oid"),
            @JoinColumn(name = "anyContainer_owner_id", referencedColumnName = "owner_id") },
            foreignKey = @ForeignKey(name = "fk_a_ext_date_owner"))
    @NotQueryable
    public RAssignmentExtension getAnyContainer() {
        return super.getAnyContainer();
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_a_ext_date_item"))
    public RExtItem getItem() {
        return super.getItem();
    }

//    @Id
//    @Column(name = "anyContainer_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
//    @NotQueryable
//    public String getOwnerOid() {
//        return super.getOwnerOid();
//    }
//
//    @Id
//    @Column(name = "anyContainer_owner_id")
//    @NotQueryable
//    public Integer getOwnerId() {
//        return super.getOwnerId();
//    }
//
//    @Id
//    @Column(name = "item_id", insertable = false, updatable = false)
//    public Long getItemId() {
//        return super.getItemId();
//    }
//
//    @Override
//    public void setItemId(Long itemId) {
//        super.setItemId(itemId);
//    }

    @Id
    @Column(name = "dateValue")
    public Timestamp getValue() {
        return value;
    }

    public void setValue(Timestamp value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RAExtDate))
            return false;
        if (!super.equals(o))
            return false;
        RAExtDate raExtDate = (RAExtDate) o;
        return Objects.equals(value, raExtDate.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
