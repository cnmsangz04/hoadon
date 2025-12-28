<template>
  <div class="container-fluid py-3">

    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h4 class="font-weight-bold">Danh sách thuế suất</h4>

      <b-button variant="success" @click="$refs.addModal.show()">
        <i class="fa fa-plus"></i> Thêm mới
      </b-button>
    </div>

    <!-- Search -->
    <div class="mb-3" style="max-width: 300px;">
      <b-input-group>
        <b-form-input
            v-model="keyword"
            placeholder="Nhập từ khóa tìm kiếm..."
        />
        <b-input-group-append>
          <b-button variant="outline-secondary">
            <i class="fa fa-search"></i>
          </b-button>
        </b-input-group-append>
      </b-input-group>
    </div>

    <!-- Table -->
    <b-table
        bordered
        hover
        small
        :items="filteredItems"
        :fields="fields"
        responsive
    >
      <!-- Trạng thái -->
      <template #cell(status)="data">
        <b-form-checkbox
            switch
            v-model="data.item.status"
            @change="toggleStatus(data.item)"
        />
      </template>

      <!-- Thao tác -->
      <template #cell(actions)="data">
        <b-dropdown right variant="link" toggle-class="text-dark p-0">
          <template #button-content>
            <i class="fa fa-ellipsis-v"></i>
          </template>

          <b-dropdown-item @click="editItem(data.item)">
            Chỉnh sửa
          </b-dropdown-item>

          <b-dropdown-item class="text-danger" @click="deleteItem(data.item)">
            Xóa
          </b-dropdown-item>
        </b-dropdown>
      </template>
    </b-table>

    <!-- Modal Thêm mới -->
    <b-modal ref="addModal" title="Thêm thuế suất mới">
      <b-card>
        <b-form @submit.prevent="addItem">
          <b-form-group label="Thuế suất">
            <b-form-input v-model="newItem.rate" required />
          </b-form-group>

          <b-form-group label="Trạng thái">
            <b-form-checkbox switch v-model="newItem.status" />
          </b-form-group>

          <b-button type="submit" variant="success">Thêm mới</b-button>
        </b-form>
      </b-card>
    </b-modal>

    <!-- Modal Chỉnh sửa -->
    <b-modal ref="editModal" title="Chỉnh sửa thuế suất">
      <b-card>
        <b-form @submit.prevent="updateItem">
          <b-form-group label="Thuế suất">
            <b-form-input v-model="editItemData.rate" required />
          </b-form-group>

          <b-form-group label="Trạng thái">
            <b-form-checkbox switch v-model="editItemData.status" />
          </b-form-group>

          <b-button type="submit" variant="primary">Cập nhật</b-button>
        </b-form>
      </b-card>
    </b-modal>

  </div>
</template>

<script>
import axios from "axios";
import toastr from "toastr";

export default {
  name: "TaxRateList",
  data() {
    return {
      keyword: "",
      fields: [
        { key: "id", label: "#", class: "text-center" },
        { key: "rate", label: "Thuế suất" },
        { key: "status", label: "Trạng thái", class: "text-center" },
        { key: "createdAt", label: "Ngày tạo" },
        { key: "actions", label: "Thao tác", class: "text-center" }
      ],
      items: [],
      newItem: {
        rate: "",
        status: true,
        createdAt: ""
      },
      editItemData: {       // <-- Phải khai báo ở đây
        id: null,
        rate: "",
        status: true,
        createdAt: ""
      }
    };
  },
  computed: {
    filteredItems() {
      if (!this.keyword) return this.items;
      const k = this.keyword.toLowerCase();
      return this.items.filter(i => i.rate.toLowerCase().includes(k));
    }
  },
  mounted() {
    this.fetchTaxRates();
  },
  methods: {
    async fetchTaxRates() {
      try {
        const res = await axios.get("/administrator/tax-rate");
        this.items = res.data.map(item => ({
          id: item.id,
          rate: item.rate,
          status: item.status,
          createdAt: this.formatDate(item.createdAt)
        }));
      } catch (e) {
        console.error("Lỗi load tax rate", e);
      }
    },

    formatDate(dateTime) {
      if (!dateTime) return "";
      const d = new Date(dateTime);
      return d.toLocaleDateString("vi-VN");
    },

    async toggleStatus(item) {
      try {
        await axios.put(`/administrator/tax-rate/${item.id}/toggle-status`);
        this.fetchTaxRates();
      } catch (error) {
        console.error("Error updating status:", error);
        alert("Cập nhật trạng thái thất bại!");
      }
    },

    editItem(item) {
      this.editItemData = { ...item }; // copy dữ liệu item vào form
      this.$refs.editModal.show();
    },

    updateItem() {
      const payload = {
        rate: this.editItemData.rate,
        status: this.editItemData.status
      };

      axios.put(`/administrator/tax-rate/${this.editItemData.id}`, payload)
          .then(() => {
            this.fetchTaxRates();
            this.$refs.editModal.hide();
            toastr.success("Cập nhật thành công!");
          })
          .catch(() => alert("Cập nhật thất bại!"));
    },

    async deleteItem(item) {
      if (!confirm("Xóa thuế suất " + item.rate + "?")) return;

      try {
        await axios.delete(`/administrator/tax-rate/${item.id}`);
        this.fetchTaxRates();
        toastr.success("Xóa thành công!");
      } catch (e) {
        console.error("Xóa thất bại", e);
      }
    },

    async addItem() {
      try {
        const payload = {
          rate: this.newItem.rate,
          status: this.newItem.status
        };

        await axios.post("/administrator/tax-rate", payload, {
          headers: { "Content-Type": "application/json" }
        });
        // Tải lại danh sách
        this.fetchTaxRates();
        // Reset form
        this.newItem = { rate: "", status: true, createdAt: "" };
        // Ẩn modal
        this.$refs.addModal.hide();
        toastr.success("Thêm thành công!");
      } catch (e) {
        console.error("Thêm mới thất bại", e);
        alert("Thêm mới thất bại!");
      }
    }
  }
};
</script>

<style scoped>
.table td,
.table th {
  vertical-align: middle;
}
</style>
