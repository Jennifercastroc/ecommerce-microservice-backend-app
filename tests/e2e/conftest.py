import os
import time
from dataclasses import dataclass
from typing import Iterable, Optional

import pytest
import requests
from requests import Response
from requests.exceptions import RequestException

DEFAULT_TIMEOUT = int(os.getenv("API_GATEWAY_TIMEOUT_SECONDS", "15"))
READINESS_TIMEOUT = int(os.getenv("API_GATEWAY_READINESS_TIMEOUT_SECONDS", "60"))
EUREKA_BASE_URL = os.getenv("EUREKA_BASE_URL", "http://localhost:8761")


def _build_url(base: str, path: str) -> str:
    return f"{base.rstrip('/')}/{path.lstrip('/')}"


@dataclass
class ApiGatewayClient:
    base_url: str
    session: requests.Session

    def request(
        self,
        method: str,
        path: str,
        *,
        expected_status: Optional[int] = 200,
        **kwargs,
    ) -> Response:
        url = _build_url(self.base_url, path)
        response = self.session.request(
            method=method.upper(),
            url=url,
            timeout=DEFAULT_TIMEOUT,
            **kwargs,
        )
        if expected_status is not None:
            assert response.status_code == expected_status, (
                f"{method.upper()} {url} respondio {response.status_code}, "
                f"esperado {expected_status}. Cuerpo: {response.text[:300]}"
            )
        return response

    def json(
        self,
        method: str,
        path: str,
        *,
        expected_status: Optional[int] = 200,
        **kwargs,
    ):
        response = self.request(
            method,
            path,
            expected_status=expected_status,
            **kwargs,
        )
        try:
            return response.json()
        except ValueError as exc:
            pytest.fail(
                f"Respuesta no es JSON valida para "
                f"{method.upper()} {path}: {exc}. "
                f"Cuerpo: {response.text[:300]}",
            )

    def wait_for(
        self,
        method: str,
        path: str,
        *,
        expected_status: int = 200,
        timeout: int = READINESS_TIMEOUT,
        interval: float = 2.0,
        **kwargs,
    ) -> Response:
        """Polls the gateway until the route responds with the expected status."""
        deadline = time.time() + timeout
        last_response: Optional[Response] = None
        last_error: Optional[BaseException] = None
        url = _build_url(self.base_url, path)
        while time.time() < deadline:
            try:
                response = self.session.request(
                    method=method.upper(),
                    url=url,
                    timeout=DEFAULT_TIMEOUT,
                    **kwargs,
                )
            except RequestException as exc:
                last_error = exc
            else:
                last_response = response
                if expected_status is None or response.status_code == expected_status:
                    return response
            time.sleep(interval)
        if last_response is not None:
            pytest.fail(
                f"{method.upper()} {url} no respondio con {expected_status} tras "
                f"{timeout}s (ultimo={last_response.status_code}, cuerpo={last_response.text[:200]})"
            )
        pytest.fail(
            f"{method.upper()} {url} no disponible tras {timeout}s "
            f"(ultimo error: {last_error})"
        )


def wait_for_http(url: str, *, expected_status: int = 200, timeout: int = READINESS_TIMEOUT, interval: float = 2.0) -> Response:
    deadline = time.time() + timeout
    last_response: Optional[Response] = None
    last_error: Optional[BaseException] = None
    while time.time() < deadline:
        try:
            response = requests.get(url, timeout=DEFAULT_TIMEOUT)
        except RequestException as exc:
            last_error = exc
        else:
            last_response = response
            if expected_status is None or response.status_code == expected_status:
                return response
        time.sleep(interval)
    if last_response is not None:
        pytest.fail(
            f"GET {url} no respondio con {expected_status} tras {timeout}s "
            f"(ultimo={last_response.status_code}, cuerpo={last_response.text[:200]})"
        )
    pytest.fail(
        f"GET {url} no disponible tras {timeout}s (ultimo error: {last_error})"
    )


def ensure_services_registered(service_ids: Iterable[str], timeout: int = 240):
    base = EUREKA_BASE_URL.rstrip('/')
    wait_for_http(f"{base}/actuator/health", timeout=timeout)
    for service_id in service_ids:
        wait_for_http(f"{base}/eureka/apps/{service_id}", timeout=timeout)


@pytest.fixture(scope="session")
def base_url() -> str:
    return os.getenv("API_GATEWAY_BASE_URL", "http://localhost:8080")


@pytest.fixture(scope="session", autouse=True)
def wait_for_gateway(base_url: str):
    deadline = time.time() + READINESS_TIMEOUT
    last_error: Optional[BaseException] = None
    with requests.Session() as session:
        health_url = _build_url(base_url, "actuator/health")
        while time.time() < deadline:
            try:
                response = session.get(health_url, timeout=DEFAULT_TIMEOUT)
            except RequestException as exc:
                last_error = exc
            else:
                if response.status_code == 200:
                    try:
                        body = response.json()
                    except ValueError:
                        body = {}
                    if body.get("status") == "UP":
                        return
            time.sleep(2)
    pytest.skip(
        f"API Gateway no disponible en {base_url} tras {READINESS_TIMEOUT}s "
        f"({last_error})",
    )


@pytest.fixture(scope="session")
def gateway_session() -> requests.Session:
    session = requests.Session()
    yield session
    session.close()


@pytest.fixture(scope="session")
def session_client(base_url: str, gateway_session: requests.Session) -> ApiGatewayClient:
    return ApiGatewayClient(base_url=base_url, session=gateway_session)


@pytest.fixture(scope="session", autouse=True)
def wait_for_core_services(session_client: ApiGatewayClient):
    ensure_services_registered([
        "PRODUCT-SERVICE",
        "USER-SERVICE",
        "ORDER-SERVICE",
        "PAYMENT-SERVICE",
        "SHIPPING-SERVICE",
    ], timeout=300)
    session_client.wait_for("get", "/product-service/api/products", timeout=300)
    session_client.wait_for("get", "/product-service/api/categories", timeout=300)
    session_client.wait_for("get", "/user-service/api/users", timeout=300)
    session_client.wait_for("get", "/order-service/api/carts", timeout=300)
    session_client.wait_for("get", "/payment-service/api/payments", timeout=300)
    session_client.wait_for("get", "/shipping-service/api/shippings", timeout=300)


@pytest.fixture
def api_client(session_client: ApiGatewayClient) -> ApiGatewayClient:
    return session_client
