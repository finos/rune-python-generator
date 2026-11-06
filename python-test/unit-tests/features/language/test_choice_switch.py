#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''switch narrows to the matching choice option, and gives nothing for an absent value'''
from rosetta_dsl.test.language.choice_switch.Bicycle import Bicycle
from rosetta_dsl.test.language.choice_switch.ElectricCar import ElectricCar
from rosetta_dsl.test.language.choice_switch.Motorcycle import Motorcycle
from rosetta_dsl.test.language.choice_switch.PetrolCar import PetrolCar
from rosetta_dsl.test.language.choice_switch.PoweredVehicle import PoweredVehicle
from rosetta_dsl.test.language.choice_switch.Size import Size
from rosetta_dsl.test.language.choice_switch.Vehicle import Vehicle
from rosetta_dsl.test.language.choice_switch.functions.ComputeMileage import ComputeMileage
from rosetta_dsl.test.language.choice_switch.functions.ComputeVehicleMileage import ComputeVehicleMileage
from rosetta_dsl.test.language.choice_switch.functions.DescribeNumber import DescribeNumber
from rosetta_dsl.test.language.choice_switch.functions.DescribeSize import DescribeSize


def test_choice_case_uses_option_attribute():
    '''a petrol car matches its case and can use its fuel capacity'''
    assert ComputeMileage(PoweredVehicle(PetrolCar=PetrolCar(fuelCapacity=10))) == 150


def test_choice_case_uses_other_option_attribute():
    '''an electric car matches its case and can use its battery capacity'''
    assert ComputeMileage(PoweredVehicle(ElectricCar=ElectricCar(batteryCapacity=20))) == 100


def test_choice_option_without_case_matches_default():
    '''a motorcycle has no case, so it matches the default'''
    assert ComputeMileage(PoweredVehicle(Motorcycle=Motorcycle(fuelCapacity=5))) == 80


def test_absent_choice_gives_nothing():
    '''an absent choice matches no case and gives nothing'''
    assert ComputeMileage(None) is None


def test_nested_choice_option():
    '''a petrol car inside the nested choice matches its own case'''
    vehicle = Vehicle(PoweredVehicle=PoweredVehicle(PetrolCar=PetrolCar(fuelCapacity=10)))
    assert ComputeVehicleMileage(vehicle) == 150


def test_nested_choice_parent_option():
    '''a motorcycle inside the nested choice matches the PoweredVehicle case'''
    vehicle = Vehicle(PoweredVehicle=PoweredVehicle(Motorcycle=Motorcycle(fuelCapacity=5)))
    assert ComputeVehicleMileage(vehicle) == 80


def test_nested_choice_other_option():
    '''a bicycle matches its case'''
    assert ComputeVehicleMileage(Vehicle(Bicycle=Bicycle(weight=12))) == 30


def test_enum_switch_matches():
    '''control: an enum case matches'''
    assert DescribeSize(Size.SMALL) == 'small'


def test_absent_enum_gives_nothing():
    '''an absent enum matches no case and gives nothing'''
    assert DescribeSize(None) is None


def test_literal_switch_matches():
    '''control: a literal case matches'''
    assert DescribeNumber(1) == 'one'


def test_absent_literal_gives_nothing():
    '''an absent value matches no case and gives nothing'''
    assert DescribeNumber(None) is None
