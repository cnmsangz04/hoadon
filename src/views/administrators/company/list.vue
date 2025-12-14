<template>
  <div class="container-fluid py-3">

    <h4 class="mb-3 font-weight-bold">Danh sách công ty</h4>

    <!-- FILTER -->
    <b-card class="mb-2">
      <b-row>
        <b-col cols="6">
          <b-form-group
            label="Từ khóa:"
            horizontal
            :label-cols="3"
            label-class="font-weight-bold text-right"
          >
            <b-form-input
              size="sm"
              v-model="filter.keyword"
              placeholder="Domain / MST"
            />
          </b-form-group>
        </b-col>

        <b-col cols="6">
          <b-form-group
            label="Trạng thái:"
            horizontal
            :label-cols="3"
            label-class="font-weight-bold text-right"
          >
            <b-form-select
              size="sm"
              v-model="filter.status"
              :options="statusOptions"
            />
          </b-form-group>
        </b-col>

        <b-col cols="12" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">
            <i class="fas fa-search"></i> Tìm kiếm
          </b-button>
          <b-button size="sm" variant="success" class="ml-2" @click="showModal()">
            <i class="fas fa-plus"></i> Thêm công ty
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- TABLE -->
    <b-table
      ref="tblCompany"
      bordered
      hover
      responsive
      small
      show-empty
      :fields="fields"
      :items="dataProvider"
      :busy.sync="isBusy"
      :per-page="list.per_page"
      :current-page="list.current_page"
      empty-text="Không có dữ liệu"
    >
      <template #cell(id)="data">
        {{ data.index + 1 + (list.current_page - 1) * list.per_page }}
      </template>

      <template #cell(company)="data">
        <div class="font-weight-bold">{{ data.item.companyName }}</div>
        <div class="text-muted small">{{ data.item.companyAddress }}</div>
      </template>

      <template #cell(status)="data">
        <b-badge variant="success" v-if="data.item.status === 'active'">
          Kích hoạt
        </b-badge>
        <b-badge variant="secondary" v-else>
          Ngưng hoạt động
        </b-badge>
      </template>

      <template #cell(option)="data">
        <b-button size="sm" variant="warning" class="mr-1" @click="editCompany(data.item)">Sửa</b-button>
        <b-button size="sm" variant="danger" @click="deleteCompany(data.item.id)">Xóa</b-button>
      </template>
    </b-table>

    <!-- PAGINATION -->
    <b-pagination
      v-model="list.current_page"
      :per-page="list.per_page"
      :total-rows="list.total"
      align="right"
      class="mt-2"
      v-if="list.total > list.per_page"
      @change="onPageChange"
    />

    <!-- Modal Thêm/Sửa -->
    <b-modal id="companyModal" ref="companyModal" title="Thêm/Sửa công ty" hide-footer>
      <b-form @submit.prevent="saveCompany">
        <!-- Mã số thuế -->
        <b-form-group label="Mã số thuế:" label-for="taxcode">
          <b-form-input id="taxcode" v-model="companyForm.taxcode" required></b-form-input>
        </b-form-group>

        <!-- Domain -->
        <b-form-group label="Domain:" label-for="domain">
          <b-form-input id="domain" v-model="companyForm.domain" required></b-form-input>
        </b-form-group>

        <!-- Domain tra cứu -->
        <b-form-group label="Domain tra cứu:" label-for="domainLookup">
          <b-form-input id="domainLookup" v-model="companyForm.domainLookup"></b-form-input>
        </b-form-group>

        <!-- Email -->
        <b-form-group label="Email:" label-for="email">
          <b-form-input id="email" v-model="companyForm.email" type="email"></b-form-input>
        </b-form-group>

        <!-- Hotline -->
        <b-form-group label="Hotline:" label-for="hotline">
          <b-form-input id="hotline" v-model="companyForm.hotline"></b-form-input>
        </b-form-group>

        <!-- Mật khẩu -->
        <b-form-group label="Mật khẩu:" label-for="password">
          <b-input-group>
            <b-form-input id="password" v-model="companyForm.password" type="password" :disabled="companyForm.id != null && !allowEditPassword"></b-form-input>
            <b-input-group-append>
              <b-button variant="secondary" @click="generatePassword">Tạo mới</b-button>
            </b-input-group-append>
          </b-input-group>
          <small class="text-muted">Nhấn "Tạo mới" để sinh mật khẩu ngẫu nhiên.</small>
        </b-form-group>

        <!-- Nhập lại mật khẩu -->
        <b-form-group label="Nhập lại mật khẩu:" label-for="confirmPassword">
          <b-form-input id="confirmPassword" v-model="confirmPassword" type="password" :disabled="companyForm.id != null && !allowEditPassword"></b-form-input>
          <b-form-invalid-feedback :state="passwordsMatch">
            Mật khẩu nhập lại không khớp.
          </b-form-invalid-feedback>
        </b-form-group>

        <!-- Trạng thái -->
        <b-form-group label="Trạng thái:" label-for="status">
          <b-form-select id="status" v-model="companyForm.status" :options="statusOptions"></b-form-select>
        </b-form-group>

        <!-- Actions -->
        <b-button type="submit" variant="primary" :disabled="!passwordsMatch">Lưu</b-button>
        <b-button type="button" variant="secondary" @click="$refs.companyModal.hide()">Hủy</b-button>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from "@/plugins/axios";

export default {
  name: "CompanyList",

  data() {
    return {
      isBusy: false,

      filter: {
        keyword: null,
        status: null,
        companyId: null
      },

      companyForm: {
        id: null,
        // giữ nguyên các trường cũ nếu có hiển thị ở bảng
        companyName: "",
        companyAddress: "",
        // các trường theo yêu cầu
        domain: "",
        domainLookup: "",
        email: "",
        hotline: "",
        taxcode: "",
        password: "",
        status: "active"
      },

      confirmPassword: "",
      allowEditPassword: true,

      statusOptions: [
        { value: "active", text: "Kích hoạt" },
        { value: "inactive", text: "Ngưng hoạt động" }
      ],

      list: {
        current_page: 1,
        per_page: 10,
        total: 0
      },

      fields: [
        { key: "id", label: "#", thStyle: { width: "50px" } },
        { key: "domain", label: "Domain" },
        { key: "company", label: "Tên công ty / Địa chỉ" },
        { key: "email", label: "Email" },
        { key: "status", label: "Trạng thái" },
        { key: "option", label: "Chức năng", thStyle: { width: "150px" } }
      ]
    };
  },

  computed: {
    passwordsMatch() {
      // Nếu không sửa mật khẩu (ví dụ edit và không chạm), cho qua
      if (!this.allowEditPassword && this.companyForm.id) return true;
      return this.companyForm.password === this.confirmPassword;
    }
  },

  methods: {
    dataProvider(ctx) {
      this.isBusy = true;
      return axios.post("/administrator/company/list", this.filter, {
        params: { page: ctx.currentPage - 1, size: this.list.per_page }
      })
      .then(res => {
        this.isBusy = false;
        this.list.total = res.data.totalElements;
        this.list.current_page = res.data.number + 1;
        return res.data.content || [];
      })
      .catch(() => {
        this.isBusy = false;
        return [];
      });
    },

    onFilter() {
      this.list.current_page = 1;
      this.$refs.tblCompany.refresh();
    },

    onPageChange(page) {
      this.list.current_page = page;
      this.$refs.tblCompany.refresh();
    },

    showModal() {
      this.companyForm = {
        id: null,
        companyName: "",
        companyAddress: "",
        domain: "",
        domainLookup: "",
        email: "",
        hotline: "",
        taxcode: "",
        password: "",
        status: "active"
      };
      this.confirmPassword = "";
      this.allowEditPassword = true;
      this.$refs.companyModal.show();
    },

    editCompany(item) {
      this.companyForm = { ...item };
      // không tự động set mật khẩu khi sửa; để trống
      this.companyForm.password = "";
      this.confirmPassword = "";
      // cho phép người dùng bật/tắt sửa mật khẩu nếu cần
      this.allowEditPassword = true;
      this.$refs.companyModal.show();
    },

    generatePassword() {
      const chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
      const length = 12;
      let pwd = "";
      for (let i = 0; i < length; i++) {
        pwd += chars.charAt(Math.floor(Math.random() * chars.length));
      }
      this.companyForm.password = pwd;
      this.confirmPassword = pwd;
    },

    saveCompany() {
      // xác thực mật khẩu khớp
      if (!this.passwordsMatch) {
        this.$bvToast && this.$bvToast.toast("Mật khẩu nhập lại không khớp", { variant: "danger" });
        return;
      }
      const payload = { ...this.companyForm };
      // nếu là cập nhật và người dùng không nhập mật khẩu, loại bỏ trường password để backend không ghi đè
      if (payload.id && !payload.password) {
        delete payload.password;
      }
      axios.post("/administrator/company/saveOrUpdate", payload)
        .then(() => {
          this.$refs.companyModal.hide();
          this.$refs.tblCompany.refresh();
        });
    },

    deleteCompany(id) {
      if(confirm("Bạn có chắc muốn xóa công ty này?")) {
        axios.delete(`/administrator/company/${id}`).then(() => {
          this.$refs.tblCompany.refresh();
        });
      }
    }
  }
};
</script>

<style scoped>
.table td {
  vertical-align: middle;
}
.ml-2 { margin-left: 0.5rem; }
.mr-1 { margin-right: 0.25rem; }
</style>