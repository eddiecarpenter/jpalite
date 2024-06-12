/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.jpalite.jqpl;

import io.jpalite.impl.JPAEntityImpl;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "RATE_PLAN")
public class RatePlan extends JPAEntityImpl
{
	@Id
	@GeneratedValue
	@Column(name = "ID", updatable = false)
	Long id;

	@Column(name = "UID", nullable = false)
	String uid;

	@Column(name = "RESOURCE_VERSION", nullable = false)
	long resourceVersion = 0;

	@Column(name = "OPERATOR_ID", nullable = false)
	long operatorId;

	@Column(name = "PLAN_NAME")
	String name;

	@Column(name = "CREATED_BY", nullable = false)
	String createdBy;

	@Column(name = "APPROVED_BY")
	String approvedBy;

	@Column(name = "EFFECTIVE_DATE", nullable = false)
	Timestamp effectiveDate;

	@Column(name = "RATE_PLAN_CONFIG", nullable = false)
	String ratePlanConfig;

	@Version
	@Column(name = "MODIFIED_ON", nullable = false)
	Timestamp modifiedOn;

	@Column(name = "CREATED_DATE", nullable = false)
	Timestamp createdDate;
}
