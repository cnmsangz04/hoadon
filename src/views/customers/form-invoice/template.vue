<template>
  <div class="p-3">
    <b-container class="mb-3">
      <b-row>
        <b-col cols="12" md="4">
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

        <b-col cols="12" md="4">
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
    </b-container>

    <b-row>
      <b-col cols="12" v-if="notify" class="text-center">
        <p class="text-danger">{{ notify }}</p>
      </b-col>

      <b-col
        cols="6"
        md="3"
        lg="3"
        xl="3"
        class="mb-3"
        v-for="(item, index) in templates"
        :key="index"
      >
        <b-card :img-src="item.photo" class="text-center">
          <div class="text-secondary">
            <p>{{ item.name }}</p>
            <b-button @click="copyTemplate(item.id)" size="sm" variant="default">
              Chọn Mẫu
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
      // Fetch templates filtered by selected category/type and only system=0
      this.notify = ''
      try {
        const params = { system: 0 }
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
          this.notify = 'Không có mẫu phù hợp. Vui lòng chọn loại hóa đơn/thuế suất khác.'
        }
      } catch (e) {
        this.templates = []
        this.notify = 'Không tải được danh sách mẫu. Vui lòng thử lại.'
      }
    },
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
</style>
