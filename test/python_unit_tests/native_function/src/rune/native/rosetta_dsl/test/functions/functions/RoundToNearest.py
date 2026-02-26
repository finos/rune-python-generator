"""
Native Python implementation of the RoundToNearest function.
"""

from decimal import Decimal

from rosetta_dsl.test.functions.RoundingModeEnum import RoundingModeEnum


def RoundToNearest(
    value: Decimal, digits: int, roundingMode: RoundingModeEnum
) -> Decimal:
    """
    Round a number to the supplied digits, using the supplied rounding mode.

    :param value: The original (unrounded) number.
    :param digits: The number of digits to round to.
    :param roundingMode: The method of rounding (Up or Down).
    :return: The rounded number.
    """
    # Map the Rosetta Enum to Python's decimal rounding modes
    if roundingMode == RoundingModeEnum.UP:
        decimal_rounding = "ROUND_UP"
    else:
        decimal_rounding = "ROUND_DOWN"

    # quantifier for decimal places, e.g. 2 -> Decimal('0.01')
    quantifier = Decimal("1").scaleb(-digits)

    return value.quantize(quantifier, rounding=decimal_rounding)
