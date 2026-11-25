package vn.hoadon.services;

public interface TaxCodeLookupService {

    Taxpayer lookup(String taxCode);

    record Taxpayer(
            String taxCode,
            String name,
            String address,
            String status,
            String taxDepartment,
            String source
    ) {
    }

    class LookupException extends RuntimeException {
        private final int status;

        public LookupException(int status, String message) {
            super(message);
            this.status = status;
        }

        public LookupException(int status, String message, Throwable cause) {
            super(message, cause);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
