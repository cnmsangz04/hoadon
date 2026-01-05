<template>
  <div class="container-fluid py-3">

    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div class="d-flex align-items-center">
        <h4 class="font-weight-bold mb-0">Template mail</h4>
      </div>

      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i> Làm mới
        </b-button>
        <b-button
            variant="success"
            :to="{ name: 'admin-email-template-create' }"
        >
          <i class="fa fa-plus"></i> Thêm mới
        </b-button>
      </div>
    </div>

    <!-- Filter -->
    <b-card class="mb-3">
      <b-row>
        <b-col md="4">
          <b-form-group label="Công ty">
            <b-form-input
                :value="company ? company.name : ''"
                readonly
                disabled
            />
          </b-form-group>
        </b-col>
      </b-row>
    </b-card>

    <!-- Table -->
    <b-card>
      <b-table
          :items="items"
          :fields="fields"
          responsive
          bordered
          small
      >
        <!-- STT -->
        <template #cell(index)="row">
          {{ row.index + 1 }}
        </template>

        <!-- Company -->
        <template #cell(companyName)="row">
          {{ row.item.companyName }}
        </template>

        <!-- Key -->
        <template #cell(key)="row">
          <div>
            <strong>{{ row.item.key }}</strong>
            <div v-if="row.item.system === 0">
              <b-badge variant="danger">Hệ thống</b-badge>
            </div>
          </div>
        </template>

        <!-- Status -->
        <template #cell(status)="row">
          <b-badge
              :variant="row.item.status === 1 ? 'success' : 'secondary'"
          >
            {{ row.item.status === 1 ? 'Đang kích hoạt' : 'Ngưng hoạt động' }}
          </b-badge>
        </template>

        <template #cell(updatedAt)="row">
          {{ formatDate(row.item.updatedAt) }}
        </template>

        <!-- Action -->
        <template #cell(actions)="row">
          <b-dropdown right variant="link" no-caret>
            <template #button-content>
              <i class="fa fa-ellipsis-h"></i>
            </template>

            <b-dropdown-item @click="edit(row.item.id)">
              Sửa
            </b-dropdown-item>

            <b-dropdown-item
                class="text-danger"
                @click="remove(row.item.id)"
            >
              Xóa
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>
    </b-card>

  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'MailTemplateList',

  data () {
    return {
      company: null,

      fields: [
        { key: 'index', label: '#', class: 'text-center' },
        { key: 'companyName', label: 'Tên công ty' },
        { key: 'key', label: 'Mã template' },
        { key: 'title', label: 'Tiêu đề' },
        { key: 'status', label: 'Trạng thái', class: 'text-center' },
        { key: 'updatedAt', label: 'Ngày cập nhật' },
        { key: 'actions', label: 'Thao tác', class: 'text-center' }
      ],

      items: []
    }
  },

  created () {
    this.initCompany()
  },

  methods: {
    initCompany () {
      const timer = setInterval(() => {
        const company = this.$app?.info?.company

        if (company && company.id) {
          clearInterval(timer)
          this.company = company
          this.fetchData()
        }
      }, 50)
    },

    fetchData () {
      axios.get('/administrator/mail-template', {
        params: {
          companyId: this.company.id
        }
      }).then(res => {
        this.items = res.data || []
      })
    },

    edit (id) {
      this.$router.push({
        name: 'admin-email-template-create',
        params: { id }
      })
    },

    remove (id) {
      this.$bvModal.msgBoxConfirm('Bạn chắc chắn muốn xóa?', {
        title: 'Xác nhận',
        okVariant: 'danger',
        okTitle: 'Xóa',
        cancelTitle: 'Hủy'
      }).then(confirm => {
        if (!confirm) return

        axios.delete(`/administrator/mail-template/${id}`)
            .then(() => {
              this.$toastr.success('Xóa thành công')
              this.fetchData()
            })
      })
    },

    formatDate(date) {
      return date ? new Date(date).toLocaleDateString("vi-VN") : "";
    },

    reload(){
      this.fetchData();
    }
  }
}
</script>


<style scoped>
.fa-ellipsis-h {
  cursor: pointer;
}
</style>
