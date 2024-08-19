package org.jpalite.impl;

import org.jpalite.test.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JPAEntityImplTest
{

    @BeforeAll
    static void beforeAll()
    {
        TestEntityMetaDataManager.init();
    }

    @Test
    void testToJson()
    {
        Company c = new Company();
        c.setId(3);
        c.setName("Test Company");

        Department d = new Department();
        d.setId(2);
        d.setName("Test Dept");
        d.setCompany(c);

        Employee e = new Employee();
        e.setAge(10);
        e.setId(1);
        e.setSalary(BigDecimal.valueOf(4000.00).setScale(4, RoundingMode.HALF_DOWN));
        e.setFullName(new FullName("Test", "Employee"));
        e.setDepartment(d);

        Phone p1 = new Phone();
        p1.setId(4);
        p1.setNumber("1234567890");
        p1.setEmployee(e);

        Phone p2 = new Phone();
        p2.setId(5);
        p2.setNumber("0987654321");
        p2.setEmployee(e);

        e.setPhones(List.of(p1, p2));

        String json = e._toJson();

        Employee e2 = new Employee();
        e2._fromJson(json);

        assertEquals(e.getId(), e2.getId());
        assertEquals(e.getAge(), e2.getAge());
        assertEquals(e.getFullName().getName(), e2.getFullName().getName());
        assertEquals(e.getFullName().getSurname(), e2.getFullName().getSurname());
        assertEquals(e.getDepartment().getId(), e2.getDepartment().getId());
        assertEquals(e.getSalary(), e2.getSalary());
    }
}
