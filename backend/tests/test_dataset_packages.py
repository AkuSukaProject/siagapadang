import hashlib

import pytest

from app.services.dataset_packages import remote_version_is_newer, verify_package


def test_verify_package_accepts_matching_size_and_checksum(tmp_path):
    package = tmp_path / "dataset.db"
    package.write_bytes(b"valid sqlite package fixture")
    checksum = hashlib.sha256(package.read_bytes()).hexdigest()

    verify_package(package, package.stat().st_size, checksum.upper())


def test_verify_package_rejects_wrong_checksum(tmp_path):
    package = tmp_path / "dataset.db"
    package.write_bytes(b"corrupted package")

    with pytest.raises(ValueError, match="Checksum"):
        verify_package(package, package.stat().st_size, "0" * 64)


def test_verify_package_rejects_wrong_size(tmp_path):
    package = tmp_path / "dataset.db"
    package.write_bytes(b"short")

    with pytest.raises(ValueError, match="Ukuran"):
        verify_package(package, 999, hashlib.sha256(package.read_bytes()).hexdigest())


def test_remote_version_comparison_prevents_downgrade():
    assert remote_version_is_newer("2026.09.16", "2026.09.13")
    assert not remote_version_is_newer("2026.09.13", "2026.09.16")
    assert not remote_version_is_newer("2026.09.13", "2026.09.13")
