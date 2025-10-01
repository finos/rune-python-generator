# pylint: disable=unused-import,missing-function-docstring,invalid-name

# Works with CDM 5.x.x

from datetime import date
from cdm.event.common.Trade import Trade
from cdm.event.common.TradeIdentifier import TradeIdentifier
from cdm.product.template.TradableProduct import TradableProduct
from cdm.product.template.Product import Product
from cdm.product.template.TradeLot import TradeLot
from cdm.base.staticdata.party.Counterparty import Counterparty
from cdm.product.common.settlement.PriceQuantity import PriceQuantity
from cdm.base.staticdata.party.CounterpartyRoleEnum import CounterpartyRoleEnum
from cdm.base.staticdata.party.Party import Party
from cdm.base.staticdata.party.PartyIdentifier import PartyIdentifier
from cdm.base.staticdata.identifier.AssignedIdentifier import AssignedIdentifier

def test_trade():
    product = Product()
    price_quantity = PriceQuantity()
    trade_lot = TradeLot(priceQuantity=[price_quantity])
    party_reference = Party(partyId=[PartyIdentifier(identifier="test")])
    counterparty = [Counterparty(role=CounterpartyRoleEnum.PARTY_1, partyReference=party_reference),
                    Counterparty(role=CounterpartyRoleEnum.PARTY_2, partyReference=party_reference)]
    tradable_product = TradableProduct(product=product, tradeLot=[trade_lot], counterparty=counterparty)
    trade_identifier=[TradeIdentifier(assignedIdentifier=[AssignedIdentifier(identifier="Acme")])]

    t = Trade(
        tradeDate=date(2023, 1, 1),
        tradableProduct=tradable_product,
        tradeIdentifier=trade_identifier
    )
    print(t.model_dump())
    print('Done!')


if __name__ == '__main__':
    test_trade()

# EOF
