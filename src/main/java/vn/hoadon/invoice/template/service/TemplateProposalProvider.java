package vn.hoadon.invoice.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import vn.hoadon.invoice.template.catalog.InvoiceCatalogSnapshot;

public interface TemplateProposalProvider {
    record Input(byte[] sourcePdf, String sourceHash, InvoiceCatalogSnapshot catalog) { }
    record Proposal(String provider, String model, JsonNode layout, JsonNode mappings, JsonNode warnings) { }
    Proposal propose(Input input);
}
