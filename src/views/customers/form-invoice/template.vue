<template>
  <div class="p-3">
    <b-container class="mb-3">
      <b-card class="filter-card shadow-sm">
        <div class="filter-header d-flex align-items-center mb-2">
          <i class="fas fa-sliders-h text-primary mr-2"></i>
          <span class="font-weight-600">Bộ lọc mẫu hóa đơn</span>
        </div>
        <b-row>
          <b-col cols="12" md="5" class="mb-2">
            <label class="filter-label">Tên mẫu</label>
            <b-input-group>
              <b-input-group-prepend is-text>
                <i class="fas fa-search text-muted"></i>
              </b-input-group-prepend>
              <b-form-input v-model.trim="keyword" placeholder="Tìm theo tên mẫu" @keyup.enter="importData" />
              <b-input-group-append>
                <b-button size="sm" variant="primary" @click="importData">Tìm</b-button>
              </b-input-group-append>
            </b-input-group>
          </b-col>

          <b-col cols="12" md="3" class="mb-2">
            <label class="filter-label">Loại hóa đơn</label>
            <v-select
              class="style-chooser"
              id="category"
              v-model="category"
              item-value="code"
              @input="importData"
              :options="options.categories"
              placeholder="Chọn loại hóa đơn"
              :reduce="(category) => category.code"
            />
          </b-col>

          <b-col cols="12" md="3" class="mb-2">
            <label class="filter-label">Loại thuế suất</label>
            <v-select
              class="style-chooser"
              id="type"
              v-model="type"
              item-value="code"
              @input="importData"
              :options="options.types"
              placeholder="Chọn loại thuế suất"
              :reduce="(type) => type.code"
            />
          </b-col>
        </b-row>
      </b-card>
    </b-container>

    <b-row>
      <b-col cols="12" v-if="notify">
        <b-alert show variant="light" class="notify-alert text-center">
          <i class="fas fa-info-circle mr-2 text-primary"></i>
          <span class="notify-text">{{ notify }}</span>
        </b-alert>
      </b-col>

      <b-col
        cols="12"
        sm="6"
        md="4"
        lg="3"
        xl="3"
        class="mb-3"
        v-for="(item, index) in templates"
        :key="index"
      >
        <b-card class="template-card h-100 text-center">
          <div class="template-thumb mb-2">
            <img :src="item.photo" alt="template" class="template-img" />
          </div>
          <div class="text-secondary">
            <p class="template-name">{{ item.name }}</p>
            <div class="mb-2">
              <b-badge :variant="categoryVariant(item.category)" class="mr-1">{{ categoryLabel(item.category) }}</b-badge>
              <b-badge :variant="typeVariant(item.type)">{{ typeLabel(item.type) }}</b-badge>
            </div>
            <b-button @click="copyTemplate(item.id)" size="sm" variant="primary">
              Chọn mẫu
            </b-button>
          </div>
        </b-card>
      </b-col>
    </b-row>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'CustomerFormInvoiceTemplate',
  data() {
    return {
      notify: '',
      keyword: '',
      category: null,
      type: null,
      templates: [],
      options: {
        categories: [
          { code: 1, label: 'Hóa đơn giá trị gia tăng' },
          { code: 2, label: 'Hóa đơn bán hàng' },
        ],
        types: [
          { code: 1, label: 'Một thuế suất' },
          { code: 2, label: 'Nhiều thuế suất' },
        ],
      },
    }
  },
  created() {
    this.importData()
  },
  methods: {
    async importData() {
      // Fetch templates filtered by name, category, type
      // Backend /form-invoices/templates endpoint only returns system=0 templates
      this.notify = ''
      try {
        const params = {}
        if (this.keyword) params.q = this.keyword
        if (this.category != null) params.category = this.category
        if (this.type != null) params.type = this.type
        const { data } = await axios.get('/form-invoices/templates', { params })
        const items = Array.isArray(data?.items) ? data.items : Array.isArray(data) ? data : []
        this.templates = items.map(it => ({
          id: it.id,
          name: it.name,
          photo: it.photo,
          type: it.type,
          category: it.category,
        }))
        if (!this.templates.length) {
          this.notify = 'Không có mẫu phù hợp. Vui lòng chọn tên/loại hóa đơn/thuế suất khác.'
        }
      } catch (e) {
        this.templates = []
        this.notify = 'Không tải được danh sách mẫu. Vui lòng thử lại.'
      }
    },
    categoryLabel(v) { return Number(v) === 1 ? 'Hóa đơn giá trị gia tăng' : Number(v) === 2 ? 'Hóa đơn bán hàng' : '—' },
    categoryVariant(v) { return Number(v) === 1 ? 'info' : Number(v) === 2 ? 'secondary' : 'light' },
    typeLabel(v) { return Number(v) === 1 ? 'Một thuế suất' : Number(v) === 2 ? 'Nhiều thuế suất' : '—' },
    typeVariant(v) { return Number(v) === 1 ? 'success' : Number(v) === 2 ? 'warning' : 'light' },
    copyTemplate(templateId) {
      // Navigate to create page with selected templateId for copying/creating
      this.$router.push({
        name: 'CustomerFormInvoiceCreate',
        query: { templateId }
      })
    },
  },
}
</script>

<style scoped>
.style-chooser { width: 100%; }
.text-secondary p { margin-bottom: 0.5rem; }

/* Filter card styling */
.filter-card { border: 1px solid #e9eef5; }
.filter-header { font-size: 14px; }
.font-weight-600 { font-weight: 600; }
.filter-label { display: inline-block; margin-bottom: 6px; color: #6b7280; font-size: 12px; }

/* Notify styling */
.notify-alert { border-color: #e9eef5; color: #4b5563; }
.notify-text { font-size: 13px; }

.template-card {
  border: 1px solid #e9eef5;
  transition: box-shadow .2s ease, transform .2s ease;
}
.template-card:hover {
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.template-thumb {
  background: #f7fafc;
  border-radius: 8px;
  padding: 8px;
}
.template-img {
  width: 100%;
  height: auto;
  display: block;
  border-radius: 6px;
}
.template-name {
  font-weight: 600;
  margin-bottom: .25rem;
}
</style>