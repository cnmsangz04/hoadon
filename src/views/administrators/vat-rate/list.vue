<template>
  <div class="container-fluid py-3">

    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="font-weight-bold mb-0">
        Danh sách thuế suất
      </h4>

      <div>
        <b-button variant="success" @click="$refs.addModal.show()">
          <i class="fa fa-plus"></i> Thêm thuế suất
        </b-button>
      </div>
    </div>

    <!-- Search -->
    <b-card class="mb-4" body-class="py-3">
      <b-row align-v="center">
        <!-- Search keyword -->
        <b-col md="4">
          <b-input-group>
            <b-input-group-prepend>
              <b-input-group-text>
                <i class="fa fa-search"></i>
              </b-input-group-text>
            </b-input-group-prepend>

            <b-form-input
                v-model="keyword"
                placeholder="Tìm theo mã / tên thuế suất"
            />
          </b-input-group>
        </b-col>

        <!-- Search button -->
        <b-col md="2">
          <b-button variant="primary" block @click="fetchTaxRates">
            Tìm kiếm
          </b-button>
        </b-col>
      </b-row>
    </b-card>


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
            :checked="data.item.status === 1"
            @change="toggleStatus(data.item)"
        />
      </template>

      <!-- Thao tác -->
      <template #cell(actions)="data">
        <b-dropdown right variant="link" toggle-class="text-dark p-0">
          <template #button-content>
            <i class="fa fa-ellipsis-v"></i>
          </template>

          <b-dropdown-item @click="openEdit(data.item)">
            Chỉnh sửa
          </b-dropdown-item>

          <b-dropdown-item class="text-danger" @click="deleteItem(data.item)">
            Xóa
          </b-dropdown-item>
        </b-dropdown>
      </template>
    </b-table>

    <!-- ================= MODAL THÊM ================= -->
    <b-modal ref="addModal" title="Thêm mới thuế suất" hide-footer>
      <b-form @submit.prevent="addItem">

        <b-form-group label="Thuế suất">
          <b-form-input
              :value="formatLabel(newItem.code)"
              readonly
          />
        </b-form-group>

        <b-form-group label="Giá trị">
          <b-form-input
              v-model.number="newItem.code"
              type="number"
              required
          />
        </b-form-group>

        <b-form-group label="Trạng thái">
          <b-form-checkbox
              switch
              v-model="newItem.status"
              :value="1"
              :unchecked-value="0"
          >
            {{ newItem.status === 1 ? 'Hiển thị' : 'Ẩn' }}
          </b-form-checkbox>
        </b-form-group>

        <b-button type="submit" variant="success">Thêm mới</b-button>
      </b-form>
    </b-modal>

    <!-- ================= MODAL SỬA ================= -->
    <b-modal ref="editModal" title="Chỉnh sửa thuế suất" hide-footer>
      <b-form @submit.prevent="updateItem">

        <b-form-group label="Thuế suất">
          <b-form-input
              :value="formatLabel(editItem.code)"
              readonly
          />
        </b-form-group>

        <b-form-group label="Giá trị">
          <b-form-input
              v-model.number="editItem.code"
              type="number"
              required
          />
        </b-form-group>

        <b-form-group label="Trạng thái">
          <b-form-checkbox
              switch
              v-model="editItem.status"
              :value="1"
              :unchecked-value="0"
          >
            {{ editItem.status === 1 ? 'Hiển thị' : 'Ẩn' }}
          </b-form-checkbox>
        </b-form-group>

        <b-button type="submit" variant="primary">Cập nhật</b-button>
      </b-form>
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
        { key: "label", label: "Thuế suất" },
        { key: "status", label: "Trạng thái", class: "text-center" },
        { key: "createdAt", label: "Ngày tạo" },
        { key: "actions", label: "Thao tác", class: "text-center" }
      ],

      items: [],

      newItem: {
        code: null,
        status: 1
      },

      editItem: {
        id: null,
        code: null,
        status: 1
      }
    };
  },

  computed: {
    filteredItems() {
      if (!this.keyword) return this.items;
      return this.items.filter(i =>
          i.label.toLowerCase().includes(this.keyword.toLowerCase())
      );
    }
  },

  mounted() {
    this.fetchTaxRates();
  },

  methods: {
    /* ====== FORMAT ====== */
    formatLabel(code) {
      if (code === null || code === "") return "";
      if (code === -1) return "KCT";
      return `${code}%`;
    },

    formatDate(date) {
      return date ? new Date(date).toLocaleDateString("vi-VN") : "";
    },

    /* ====== API ====== */
    async fetchTaxRates() {
      const res = await axios.get("/administrator/tax-rate");
      this.items = res.data.map(i => ({
        id: i.id,
        code: i.code,
        label: i.label,
        status: i.status,
        createdAt: this.formatDate(i.createdAt)
      }));
    },

    async toggleStatus(item) {
      await axios.put(`/administrator/tax-rate/${item.id}/toggle-status`);
      this.fetchTaxRates();
      toastr.success("Cập nhật trạng thái thành công");
    },

    openEdit(item) {
      this.editItem = { ...item };
      this.$refs.editModal.show();
    },

    async addItem() {
      await axios.post("/administrator/tax-rate", {
        code: this.newItem.code,
        label: this.formatLabel(this.newItem.code),
        status: this.newItem.status
      });

      this.fetchTaxRates();
      this.newItem = { code: null, status: 1 };
      this.$refs.addModal.hide();
      toastr.success("Thêm thành công");
    },

    async updateItem() {
      await axios.put(`/administrator/tax-rate/${this.editItem.id}`, {
        code: this.editItem.code,
        label: this.formatLabel(this.editItem.code),
        status: this.editItem.status
      });

      this.fetchTaxRates();
      this.$refs.editModal.hide();
      toastr.success("Cập nhật thành công");
    },

    async deleteItem(item) {
      if (!confirm("Xóa thuế suất " + item.label + "?")) return;
      await axios.delete(`/administrator/tax-rate/${item.id}`);
      this.fetchTaxRates();
      toastr.success("Xóa thành công");
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
