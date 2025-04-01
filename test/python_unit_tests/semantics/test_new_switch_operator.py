'''switch unit tests'''
import pytest

from rosetta_dsl.test.semantic.new_switch_operator.SwitchTestLiteral import SwitchTestLiteral
from rosetta_dsl.test.semantic.new_switch_operator.SwitchTestChoice import SwitchTestChoice
from rosetta_dsl.test.semantic.new_switch_operator.CollateralCriteria import CollateralCriteria
from rosetta_dsl.test.semantic.new_switch_operator.AllCriteria import AllCriteria
from rosetta_dsl.test.semantic.new_switch_operator.SwitchTestEnum import SwitchTestEnum
from rosetta_dsl.test.semantic.new_switch_operator.DayCountFractionEnum import DayCountFractionEnum

def test_switch_passes():
    switch_test= SwitchTestLiteral(a=2)
    switch_test.validate_model()

def test_switch_fails ():
    switch_test = SwitchTestLiteral(a=-1)
    with pytest.raises(Exception):
        switch_test.validate_model()

def test_switch_choice_guard_passes():
    all_criteria= AllCriteria()
    input_criteria=CollateralCriteria(AllCriteria=all_criteria)
    switch_choice_guard_test= SwitchTestChoice(inputCriteria=input_criteria)
    switch_choice_guard_test.validate_model()
def test_switch_choice_guard_fails ():
    input_criteria = CollateralCriteria()
    switch_choice_guard_test = SwitchTestChoice(inputCriteria=input_criteria)
    with pytest.raises(Exception):
        switch_choice_guard_test.validate_model()

def test_switch_enum_guard_passes():
    dcf= DayCountFractionEnum(DayCountFractionEnum.ACT_360)
    switch_enum_guard_test=SwitchTestEnum(inputEnum=dcf)
    switch_enum_guard_test.validate_model()

def test_switch_enum_guard_fails():
    dcf= DayCountFractionEnum(DayCountFractionEnum.ACT_364)
    switch_enum_guard_test=SwitchTestEnum(inputEnum=dcf)
    with pytest.raises(Exception):
        switch_enum_guard_test.validate_model()

if __name__ == "__main__":
    test_switch_passes()
    test_switch_fails()
    test_switch_choice_guard_passes()
    test_switch_choice_guard_fails()
    test_switch_enum_guard_passes()
    test_switch_enum_guard_fails()