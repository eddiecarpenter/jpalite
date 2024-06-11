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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import io.jpalite.impl.JPAEntityImpl;

import java.math.BigDecimal;

/**
 *
 */
@Entity
@Table(name = "EMPLOYEE")
@Data
public class Employee1 extends JPAEntityImpl
{

	@Id
	@GeneratedValue
	@Column(name = "IRN")
	private int id;

	@Column(name = "NAME")
	private String name;

	@Column(name = "AGE")
	private int age;

	@Column(name = "SALARY")
	@Basic(fetch = FetchType.LAZY)
	private BigDecimal salary;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "DEPT")
	private Department1 department;
}//Employee

//--------------------------------------------------------------------[ End ]---
