"""
Locust entry point for exercising the ecommerce API Gateway.

Run with:
    locust -f tests/perf/locustfile.py --headless -u 50 -r 5 --run-time 5m

Export LOCUST_HOST or pass --host to point at the gateway base URL.
"""
import os
from typing import Dict, Optional

from locust import HttpUser, between, task


class ApiGatewayUser(HttpUser):
    """
    Basic workload that mixes read-heavy traffic with occasional writes.

    The host is resolved by Locust: use LOCUST_HOST or --host to set the API Gateway URL,
    e.g. http://localhost:8080.
    """

    wait_time = between(1, 3)

    def on_start(self) -> None:
        self._product_payload = self._build_product_payload()

    @staticmethod
    def _build_product_payload() -> Dict[str, object]:
        """Genera un payload simple para crear productos."""
        # Nota: los IDs reales de categorías deberán obtenerse dinámicamente si se requieren valores válidos.
        category_id = int(os.getenv("LOCUST_CATEGORY_ID", "1"))
        return {
            "productTitle": "Locust Load Test Product",
            "imageUrl": "http://example.com/locust.png",
            "sku": "LOCUST-SKU",
            "priceUnit": 99.99,
            "quantity": 10,
            "category": {
                "categoryId": category_id,
            },
        }

    def _post_product(self) -> Optional[int]:
        response = self.client.post(
            "/product-service/api/products",
            json=self._product_payload,
            name="POST /product-service/api/products",
        )
        if response.status_code < 300:
            return response.json().get("productId")
        return None

    @task(5)
    def list_products(self):
        self.client.get(
            "/product-service/api/products",
            name="GET /product-service/api/products",
        )

    @task(3)
    def list_categories(self):
        self.client.get(
            "/product-service/api/categories",
            name="GET /product-service/api/categories",
        )

    @task(1)
    def create_product(self):
        product_id = self._post_product()
        if product_id is not None:
            self.client.delete(
                f"/product-service/api/products/{product_id}",
                name="DELETE /product-service/api/products/{id}",
                catch_response=True,
            )
