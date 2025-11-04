import uuid

import pytest

from .conftest import ApiGatewayClient


pytestmark = pytest.mark.e2e


def test_gateway_healthcheck(api_client: ApiGatewayClient):
    body = api_client.json("get", "actuator/health")
    assert body.get("status") == "UP"


def test_product_catalog_listing(api_client: ApiGatewayClient):
    payload = api_client.json("get", "/product-service/api/products")
    collection = payload.get("collection") or []
    assert isinstance(collection, list)


def test_category_listing(api_client: ApiGatewayClient):
    payload = api_client.json("get", "/product-service/api/categories")
    collection = payload.get("collection") or []
    assert isinstance(collection, list)


def test_product_category_lifecycle(api_client: ApiGatewayClient):
    suffix = uuid.uuid4().hex[:8]
    category_id = None
    product_id = None
    try:
        category_payload = {
            "categoryTitle": f"E2E Category {suffix}",
            "imageUrl": f"https://example.com/categories/{suffix}.png",
        }
        category = api_client.json(
            "post",
            "/product-service/api/categories",
            json=category_payload,
        )
        category_id = category.get("categoryId")
        assert category_id, "Categoria no devolvio categoryId"

        product_payload = {
            "productTitle": f"E2E Product {suffix}",
            "imageUrl": f"https://example.com/products/{suffix}.png",
            "sku": f"E2E-SKU-{suffix}",
            "priceUnit": 19.99,
            "quantity": 5,
            "category": {"categoryId": category_id},
        }
        product = api_client.json(
            "post",
            "/product-service/api/products",
            json=product_payload,
        )
        product_id = product.get("productId")
        assert product_id, "Producto no devolvio productId"
        assert product.get("category", {}).get("categoryId") == category_id

        fetched = api_client.json(
            "get",
            f"/product-service/api/products/{product_id}",
        )
        assert fetched.get("productId") == product_id
        assert fetched.get("category", {}).get("categoryId") == category_id

        catalog = api_client.json("get", "/product-service/api/products")
        catalog_items = catalog.get("collection") or []
        assert any(item.get("productId") == product_id for item in catalog_items)

        update_payload = {
            **product_payload,
            "productId": product_id,
            "priceUnit": 29.99,
            "quantity": 10,
        }
        updated = api_client.json(
            "put",
            "/product-service/api/products",
            json=update_payload,
        )
        assert updated.get("priceUnit") == 29.99
        assert updated.get("quantity") == 10
        refreshed = api_client.json(
            "get",
            f"/product-service/api/products/{product_id}",
        )
        assert refreshed.get("priceUnit") == 29.99
        assert refreshed.get("quantity") == 10
    finally:
        if product_id is not None:
            try:
                api_client.request(
                    "delete",
                    f"/product-service/api/products/{product_id}",
                )
            except AssertionError:
                pass
        if category_id is not None:
            try:
                api_client.request(
                    "delete",
                    f"/product-service/api/categories/{category_id}",
                )
            except AssertionError:
                pass


def test_checkout_flow(api_client: ApiGatewayClient):
    suffix = uuid.uuid4().hex[:8]
    created = {}

    def _remember(name, value):
        if value:
            created[name] = value

    try:
        user_payload = {
            "firstName": f"E2E{suffix}",
            "lastName": "User",
            "email": f"e2e_{suffix}@example.com",
            "phone": "555-0101",
            "credential": {
                "username": f"e2e_{suffix}",
                "password": "P@ssw0rd!",
                "roleBasedAuthority": "ROLE_USER",
                "isEnabled": True,
                "isAccountNonExpired": True,
                "isAccountNonLocked": True,
                "isCredentialsNonExpired": True,
            },
        }
        user = api_client.json(
            "post",
            "/user-service/api/users",
            json=user_payload,
        )
        user_id = user.get("userId")
        assert user_id, "Usuario no devolvio userId"
        _remember("user", user_id)

        api_client.wait_for("get", "/order-service/api/carts")

        cart = api_client.json(
            "post",
            "/order-service/api/carts",
            json={"userId": user_id},
        )
        cart_id = cart.get("cartId")
        assert cart_id, "Carrito no devolvio cartId"
        _remember("cart", cart_id)

        category = api_client.json(
            "post",
            "/product-service/api/categories",
            json={
                "categoryTitle": f"E2E Checkout {suffix}",
                "imageUrl": f"https://example.com/categories/{suffix}.png",
            },
        )
        category_id = category.get("categoryId")
        assert category_id
        _remember("category", category_id)

        product = api_client.json(
            "post",
            "/product-service/api/products",
            json={
                "productTitle": f"E2E Checkout Product {suffix}",
                "imageUrl": f"https://example.com/products/{suffix}.png",
                "sku": f"E2E-ORDER-SKU-{suffix}",
                "priceUnit": 49.5,
                "quantity": 20,
                "category": {"categoryId": category_id},
            },
        )
        product_id = product.get("productId")
        assert product_id
        _remember("product", product_id)

        order = api_client.json(
            "post",
            "/order-service/api/orders",
            json={
                "orderDesc": f"E2E Order {suffix}",
                "orderFee": 99.9,
                "cart": {"cartId": cart_id},
            },
        )
        order_id = order.get("orderId")
        assert order_id
        assert order.get("cart", {}).get("cartId") == cart_id
        _remember("order", order_id)

        resolved_cart = api_client.json(
            "get",
            f"/order-service/api/carts/{cart_id}",
        )
        assert resolved_cart.get("user", {}).get("userId") == user_id

        order_item = api_client.json(
            "post",
            "/shipping-service/api/shippings",
            json={
                "productId": product_id,
                "orderId": order_id,
                "orderedQuantity": 2,
            },
        )
        assert order_item.get("orderedQuantity") == 2
        _remember("order_item", (order_id, product_id))

        shipping_collection = api_client.json(
            "get",
            "/shipping-service/api/shippings",
        )
        shipping_items = shipping_collection.get("collection") or []
        assert any(
            item.get("orderId") == order_id and item.get("productId") == product_id
            for item in shipping_items
        )

        payment = api_client.json(
            "post",
            "/payment-service/api/payments",
            json={
                "isPayed": True,
                "paymentStatus": "COMPLETED",
                "order": {"orderId": order_id},
            },
        )
        payment_id = payment.get("paymentId")
        assert payment_id
        assert payment.get("order", {}).get("orderId") == order_id
        _remember("payment", payment_id)

        fetched_payment = api_client.json(
            "get",
            f"/payment-service/api/payments/{payment_id}",
        )
        assert fetched_payment.get("paymentStatus") == "COMPLETED"
        assert fetched_payment.get("order", {}).get("orderId") == order_id
    finally:
        payment_id = created.get("payment")
        if payment_id:
            try:
                api_client.request(
                    "delete",
                    f"/payment-service/api/payments/{payment_id}",
                )
            except AssertionError:
                pass
        order_item_ids = created.get("order_item")
        if order_item_ids:
            order_id, product_id = order_item_ids
            try:
                api_client.request(
                    "delete",
                    f"/shipping-service/api/shippings/{order_id}/{product_id}",
                )
            except AssertionError:
                pass
        order_id = created.get("order")
        if order_id:
            try:
                api_client.request(
                    "delete",
                    f"/order-service/api/orders/{order_id}",
                )
            except AssertionError:
                pass
        cart_id = created.get("cart")
        if cart_id:
            try:
                api_client.request(
                    "delete",
                    f"/order-service/api/carts/{cart_id}",
                )
            except AssertionError:
                pass
        product_id = created.get("product")
        if product_id:
            try:
                api_client.request(
                    "delete",
                    f"/product-service/api/products/{product_id}",
                )
            except AssertionError:
                pass
        category_id = created.get("category")
        if category_id:
            try:
                api_client.request(
                    "delete",
                    f"/product-service/api/categories/{category_id}",
                )
            except AssertionError:
                pass
        user_id = created.get("user")
        if user_id:
            try:
                api_client.request(
                    "delete",
                    f"/user-service/api/users/{user_id}",
                )
            except AssertionError:
                pass
