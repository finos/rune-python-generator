from rosetta_dsl.test.functions.add_operation.UnitType import UnitType
from rosetta_dsl.test.functions.add_operation.Quantity import Quantity
from rosetta_dsl.test.functions.add_operation.functions.FilterQuantity import (
    FilterQuantity,
)


def test_add_operation():
    """Test add operation"""
    fx_eur = UnitType(currency="EUR")
    fx_jpy = UnitType(currency="JPY")
    fx_usd = UnitType(currency="USD")
    list_of_quantities = [
        Quantity(unit=fx_eur),
        Quantity(unit=fx_jpy),
        Quantity(unit=fx_usd),
    ]
    fq = FilterQuantity(quantities=list_of_quantities, unit=fx_jpy)
    assert len(fq) == 1
    assert fq[0].unit.currency == "JPY"
