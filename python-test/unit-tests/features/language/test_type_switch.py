#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''switch on a type with subtypes matches the value's type'''
from rosetta_dsl.test.language.type_switch.Contractor import Contractor
from rosetta_dsl.test.language.type_switch.Employee import Employee
from rosetta_dsl.test.language.type_switch.Manager import Manager
from rosetta_dsl.test.language.type_switch.Payroll import Payroll
from rosetta_dsl.test.language.type_switch.Person import Person
from rosetta_dsl.test.language.type_switch.functions.ComputeMonthlyWage import ComputeMonthlyWage
from rosetta_dsl.test.language.type_switch.functions.DescribeMostSpecificFirst import (
    DescribeMostSpecificFirst)


def test_case_matches_subtype():
    '''an employee matches the Employee case and can use its salary'''
    assert ComputeMonthlyWage(Employee(salary=3000)) == 3000


def test_case_uses_attributes_of_its_type():
    '''a contractor matches the Contractor case and can use its hourly rate'''
    assert ComputeMonthlyWage(Contractor(hourlyRate=10)) == 8000


def test_parent_type_matches_default():
    '''a person who is neither matches the default'''
    assert ComputeMonthlyWage(Person(name='Ann')) == 0


def test_absent_input_gives_nothing():
    '''an absent value matches no case and gives nothing'''
    assert ComputeMonthlyWage(None) is None


def test_subtype_of_case_type_matches():
    '''a manager is an employee, so it matches the Employee case'''
    assert ComputeMonthlyWage(Manager(salary=5000, bonus=100)) == 5000


def test_most_specific_case_first():
    '''with the subtype case first, a manager matches it'''
    assert DescribeMostSpecificFirst(Manager(salary=5000, bonus=100)) == 'manager'
    assert DescribeMostSpecificFirst(Employee(salary=3000)) == 'employee'
    assert DescribeMostSpecificFirst(Person()) == 'person'


def test_type_switch_in_condition_passes():
    '''the condition sees the employee's salary'''
    assert not Payroll(person=Employee(salary=3000)).validate_conditions(raise_exc=False)


def test_type_switch_in_condition_fails():
    '''the condition sees the contractor's rate and rejects it'''
    assert len(Payroll(person=Contractor(hourlyRate=0)).validate_conditions(raise_exc=False)) == 1
