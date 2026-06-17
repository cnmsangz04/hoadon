<template>
  <div class="container-fluid py-3 bank-list">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">Danh sách ngân hàng</h4>
      </div>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i> Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="openCreate">
          <i class="fas fa-plus"></i> Thêm ngân hàng
        </b-button>
      </div>
    </div>

    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="4" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input 
              v-model.trim="filters.keyword" 
              placeholder="Tìm theo Mã / Tên ngân hàng" 
              @keyup.enter="applyFilters" 
            />
          </b-input-group>
        </b-col>

        <b-col md="4" class="mb-2">
          <b-form-select v-model="filters.status" :options="statusOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="4" class="text-right mb-2">
          <b-button size="sm" variant="primary" @click="applyFilters">
            Tìm kiếm
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="shadow-sm">
      <b-table
        bordered hover responsive small show-empty
        :items="list.data"
        :fields="fields"
        :busy="isBusy"
        empty-text="Không có dữ liệu"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(abbreviation)="data">
          <b-badge variant="light" class="text-primary font-weight-bold border">
            {{ data.item.abbreviation }}
          </b-badge>
        </template>

        <template #cell(name)="data">
          <div class="font-weight-bold">{{ data.item.name }}</div>
        </template>

        <template #cell(status)="data">
          <b-badge :variant="data.item.status == 1 ? 'success' : 'secondary'">
            {{ data.item.status == 1 ? 'Hoạt động' : 'Đã khóa' }}
          </b-badge>
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content>
              <i class="fas fa-ellipsis-h text-muted"></i>
            </template>
            <b-dropdown-item @click="openEdit(item)">
              <i class="fas fa-edit mr-2"></i>Cập nhật
            </b-dropdown-item>
            <b-dropdown-item @click="toggleLock(item)">
              <i class="fas mr-2" :class="item.status == 1 ? 'fa-lock text-warning' : 'fa-unlock text-success'"></i>
              {{ item.status == 1 ? 'Khóa' : 'Mở khóa' }}
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="list.total"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>

    <b-modal
      ref="modalForm"
      :title="form.id ? 'Cập nhật ngân hàng' : 'Thêm ngân hàng mới'"
      ok-title="Lưu lại"
      cancel-title="Hủy"
      @ok.prevent="saveData"
      :busy="isSaving"
    >
      <form ref="formRef" novalidate @submit.stop.prevent="saveData">
        <b-form-group label="Mã ngân hàng (Viết tắt)" label-for="input-abbr" :state="state('abbreviation')">
          <b-form-input
            id="input-abbr"
            v-model.trim="form.abbreviation"
            placeholder="VD: VCB, ACB..."
            required
            :state="state('abbreviation')"
          ></b-form-input>
          <b-form-invalid-feedback :state="state('abbreviation')">
            {{ invalidFeedback('abbreviation') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Tên đầy đủ" label-for="input-name" :state="state('name')">
          <b-form-input
            id="input-name"
            v-model.trim="form.name"
            placeholder="VD: Ngân hàng TMCP Ngoại thương..."
            required
            :state="state('name')"
          ></b-form-input>
          <b-form-invalid-feedback :state="state('name')">
            {{ invalidFeedback('name') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Trạng thái">
          <b-form-checkbox v-model="form.status" value="1" unchecked-value="0" switch>
            {{ form.status == 1 ? 'Đang hoạt động' : 'Tạm khóa' }}
          </b-form-checkbox>
        </b-form-group>
      </form>
    </b-modal>

  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { pageFrom, pageItems, pageTo, pageTotal } from '@/utils/pagination'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'BankList',
  components: { PaginationBar },
  data() {
    return {
      isBusy: false,
      isSaving: false,
      list: {
        current_page: 1,
        data: [],
        per_page: 10,
        total: 0,
        from: 0,
        to: 0
      },
      pageSizes: [10, 20, 50, 100],
      filters: {
        keyword: '',
        status: null
      },
      statusOptions: [
        { value: 1, text: 'Hoạt động' },
        { value: 0, text: 'Đã khóa' }
      ],
      fields: [
        { key: 'index', label: '#', thStyle: { width: '50px' }, class: 'text-center' },
        { key: 'abbreviation', label: 'Mã NH', sortable: true },
        { key: 'name', label: 'Tên Ngân Hàng', sortable: true },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '120px' }, class: 'text-center' },
        { key: 'option', label: 'Thao tác', thStyle: { width: '100px' }, class: 'text-right' }
      ],
      form: {
        id: null,
        abbreviation: '',
        name: '',
        status: 1
      },
      errors: {}
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.isBusy = true;
      try {
        const rawStatus = this.filters.status;
        const cleanStatus = (rawStatus === null || rawStatus === '') ? null : Number(rawStatus);

        const filterBody = {
          keyword: this.filters.keyword || null,
          status: cleanStatus
        };

        const res = await axios.post('/administrator/bank/list', filterBody, {
          params: {
            page: this.list.current_page - 1,
            size: this.list.per_page
          }
        });

        const d = res.data;
        
        //	Cập nhật dữ liệu vào bảng
			this.list.data = pageItems(d);
			this.list.total = pageTotal(d);

        // Tính toán hiển thị "Từ ... đến ..."
			this.list.from = pageFrom(d, this.list.current_page, this.list.per_page)
			const numberOfElements = Array.isArray(this.list.data) ? this.list.data.length : 0
			this.list.to = pageTo(d, numberOfElements, this.list.current_page, this.list.per_page)

      } catch (error) {
        console.error("Lỗi tải dữ liệu:", error);
        this.$bvToast.toast('Không thể tải danh sách ngân hàng', { variant: 'danger' });
      } finally {
        this.isBusy = false;
      }
    },
    
    reload() {
      this.filters.keyword = '';
      this.filters.status = null;
      this.list.current_page = 1;
      this.loadData();
    },

    applyFilters() {
      this.list.current_page = 1;
      this.loadData();
    },

    onPageChange(page) {
      this.list.current_page = page; 
      this.loadData();
    },

    onPageSizeChange(size) {
      this.list.per_page = size;
      this.list.current_page = 1;
      this.loadData();
    },

    openCreate() {
      this.form = { id: null, abbreviation: '', name: '', status: 1 };
      this.errors = {};
      this.$refs.modalForm.show();
    },

    openEdit(item) {
      this.form = { ...item }; 
      this.errors = {};
      this.$refs.modalForm.show();
    },

	async saveData() {
	  if (!this.validateForm()) {
	    return;
	  }

	  this.isSaving = true;
	  try {
	    const response = await axios.post('/administrator/bank/save', this.form);

	    this.$bvToast.toast(this.form.id ? 'Cập nhật thành công' : 'Thêm mới thành công', { 
	      variant: 'success', 
	      title: 'Thông báo' 
	    });

	    this.$refs.modalForm.hide();
	    this.loadData();

	  } catch (error) {
	    const errorMsg = error.response?.data || 'Đã có lỗi xảy ra khi lưu dữ liệu';
	    this.$bvToast.toast(errorMsg, { 
	      variant: 'danger', 
	      title: 'Lỗi' 
	    });
	  } finally {
	    this.isSaving = false;
	  }
	},

    validateForm() {
      const errors = {};
      if (!String(this.form.abbreviation || '').trim()) {
        errors.abbreviation = ['Vui lòng nhập mã ngân hàng'];
      } else if (!/^[A-Za-z0-9_-]{2,20}$/.test(String(this.form.abbreviation).trim())) {
        errors.abbreviation = ['Mã ngân hàng chỉ gồm chữ, số, gạch ngang hoặc gạch dưới'];
      }
      if (!String(this.form.name || '').trim()) {
        errors.name = ['Vui lòng nhập tên đầy đủ'];
      }
      this.errors = errors;
      return Object.keys(errors).length === 0;
    },

    state(field) {
      return Object.prototype.hasOwnProperty.call(this.errors, field) ? false : null;
    },

    invalidFeedback(field) {
      const value = this.errors[field];
      return Array.isArray(value) ? value.join(' ') : (value || '');
    },

    async toggleLock(item) {
      try {
        const newStatus = item.status == 1 ? 0 : 1;
        await axios.post('/administrator/bank/save', { ...item, status: newStatus });
        this.$bvToast.toast('Cập nhật trạng thái thành công', { variant: 'success' });
        this.loadData();
      } catch (e) {
        this.$bvToast.toast('Lỗi cập nhật trạng thái', { variant: 'danger' });
      }
    }
  }
}
</script>

<style scoped>
.bank-list .card { border-radius: 8px; border: none; }
.btn-outline-primary:hover { background-color: #e8f0fe; color: #007bff; }
</style>
