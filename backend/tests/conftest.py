import pytest
from sqlalchemy.ext.compiler import compiles
from sqlalchemy.sql.type_api import UserDefinedType

try:
    from geoalchemy2.types import Geometry
    @compiles(Geometry, "sqlite")
    def compile_geometry_sqlite(type_, compiler, **kw):
        return "TEXT"
        
    import geoalchemy2.admin.dialects.sqlite
    geoalchemy2.admin.dialects.sqlite.after_create = lambda *args, **kwargs: None
    geoalchemy2.admin.dialects.sqlite.before_drop = lambda *args, **kwargs: None
except ImportError:
    pass

from app.database import engine
from app.models import domain

@pytest.fixture(scope="session", autouse=True)
def setup_test_db():
    domain.Base.metadata.create_all(bind=engine)
    yield
    # JANGAN HAPUS TABEL DI PRODUCTION!
    # domain.Base.metadata.drop_all(bind=engine)
