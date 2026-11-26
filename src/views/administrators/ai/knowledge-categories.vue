<template>
  <div class="container-fluid py-3 knowledge-category-page">
    <div class="d-flex align-items-center justify-content-between flex-wrap mb-3 page-heading">
      <h4 class="mb-0 font-weight-bold">{{ $t('aiKnowledge.categories.title') }}</h4>
      <div class="page-actions">
        <b-button size="sm" variant="outline-primary" :disabled="loading" @click="loadCategories">
          <b-spinner v-if="loading" small class="mr-1" />
          <i v-else class="fas fa-sync-alt mr-1"></i>{{ $t('aiKnowledge.categories.refresh') }}
        </b-button>
        <b-button size="sm" variant="success" @click="openCreate">
          <i class="fas fa-plus mr-1"></i>{{ $t('aiKnowledge.categories.add') }}
        </b-button>
      </div>
    </div>

    <b-card class="mb-3 shadow-sm filter-card">
      <b-row>
        <b-col cols="12" md="6" lg="4">
          <b-input-group>
            <b-input-group-prepend is-text><i class="fas fa-search text-muted"></i></b-input-group-prepend>
            <b-form-input v-model.trim="keyword" :placeholder="$t('aiKnowledge.categories.searchPlaceholder')" />
          </b-input-group>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="shadow-sm category-table-card">
      <div class="d-flex align-items-center justify-content-between mb-2">
        <span class="text-muted small"><i class="fas fa-tags mr-1"></i>{{ $t('aiKnowledge.categories.count', { count: filteredCategories.length }) }}</span>
      </div>
      <b-table bordered hover responsive small show-empty class="mb-0 table-modern table-compact"
               :busy="loading" :items="filteredCategories" :fields="fields" :empty-text="$t('aiKnowledge.categories.empty')">
        <template #table-busy>
          <div class="text-center py-3"><b-spinner small class="mr-2" />{{ $t('aiKnowledge.categories.loading') }}</div>
        </template>
        <template #cell(index)="{ index }">{{ index + 1 }}</template>
        <template #cell(code)="{ item }">
          <b-badge variant="light" class="border text-primary">{{ item.code }}</b-badge>
        </template>
        <template #cell(name)="{ item }">
          <div class="font-weight-bold">{{ item.name }}</div>
          <div v-if="item.description" class="small text-muted category-description">{{ item.description }}</div>
        </template>
        <template #cell(active)="{ item }">
          <b-badge :variant="item.active ? 'success' : 'secondary'">{{ item.active ? $t('aiKnowledge.categories.enabled') : $t('aiKnowledge.categories.disabled') }}</b-badge>
        </template>
        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content><i class="fas fa-ellipsis-h text-muted"></i></template>
            <b-dropdown-item @click="openEdit(item)"><i class="fas fa-edit mr-2"></i>{{ $t('aiKnowledge.categories.update') }}</b-dropdown-item>
            <b-dropdown-divider />
            <b-dropdown-item class="text-danger" @click="deleteCategory(item)">
              <i class="far fa-trash-alt mr-2"></i>{{ $t('aiKnowledge.categories.delete') }}
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>
    </b-card>

    <b-modal ref="categoryModal" :title="form.id ? $t('aiKnowledge.categories.editTitle') : $t('aiKnowledge.categories.addTitle')"
             hide-footer @hidden="resetForm">
      <b-form id="knowledge-category-form" @submit.prevent="saveCategory">
        <b-form-group :label="$t('aiKnowledge.categories.code')" label-for="knowledge-category-code">
          <b-form-input id="knowledge-category-code" v-model.trim="form.code" maxlength="80"
                        :placeholder="$t('aiKnowledge.categories.codePlaceholder')" required />
        </b-form-group>
        <b-form-group :label="$t('aiKnowledge.categories.name')" label-for="knowledge-category-name">
          <b-form-input id="knowledge-category-name" v-model.trim="form.name" maxlength="255"
                        :placeholder="$t('aiKnowledge.categories.namePlaceholder')" required />
        </b-form-group>
        <b-form-group :label="$t('aiKnowledge.categories.description')" label-for="knowledge-category-description">
          <b-form-textarea id="knowledge-category-description" v-model.trim="form.description" maxlength="1000"
                           rows="3" :placeholder="$t('aiKnowledge.categories.descriptionPlaceholder')" />
        </b-form-group>
        <b-form-group class="mb-0">
          <b-form-checkbox v-model="form.active" switch>{{ $t('aiKnowledge.categories.active') }}</b-form-checkbox>
        </b-form-group>
      </b-form>
      <div class="modal-actions">
        <b-button size="sm" variant="outline-secondary" @click="$refs.categoryModal.hide()">{{ $t('common.cancel') }}</b-button>
        <b-button size="sm" variant="primary" type="submit" form="knowledge-category-form" :disabled="!canSave">
          <b-spinner v-if="saving" small class="mr-1" />
          <i v-else class="fas fa-save mr-1"></i>{{ form.id ? $t('aiKnowledge.categories.saveChanges') : $t('aiKnowledge.categories.add') }}
        </b-button>
      </div>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { toastSuccess } from '@/utils/toast'

const emptyForm = () => ({ id: null, code: '', name: '', description: '', active: true })

export default {
  name: 'AdminAiKnowledgeCategories',
  data() {
    return {
      loading: false,
      saving: false,
      keyword: '',
      categories: [],
      form: emptyForm(),
    }
  },
  computed: {
    fields() {
      return [
        { key: 'index', label: '#', thStyle: { width: '48px' }, tdClass: 'text-center' },
        { key: 'code', label: this.$t('aiKnowledge.categories.columns.code'), thStyle: { width: '180px' } },
        { key: 'name', label: this.$t('aiKnowledge.categories.columns.name') },
        { key: 'active', label: this.$t('aiKnowledge.categories.columns.status'), thStyle: { width: '120px' }, tdClass: 'text-center' },
        { key: 'option', label: '', thStyle: { width: '52px' }, tdClass: 'text-center' },
      ]
    },
    filteredCategories() {
      const query = this.keyword.trim().toLocaleLowerCase('vi')
      if (!query) return this.categories
      return this.categories.filter(item => `${item.code || ''} ${item.name || ''}`.toLocaleLowerCase('vi').includes(query))
    },
    canSave() {
      return !this.saving && /^[A-Za-z0-9_-]{2,80}$/.test(this.form.code) && !!this.form.name.trim()
    },
  },
  mounted() {
    this.loadCategories()
  },
  methods: {
    async loadCategories() {
      this.loading = true
      try {
        const { data } = await axios.get('/administrator/ai/knowledge/categories')
        this.categories = data || []
      } finally {
        this.loading = false
      }
    },
    openCreate() {
      this.form = emptyForm()
      this.$refs.categoryModal.show()
    },
    openEdit(item) {
      this.form = {
        id: item.id,
        code: item.code || '',
        name: item.name || '',
        description: item.description || '',
        active: item.active !== false,
      }
      this.$refs.categoryModal.show()
    },
    resetForm() {
      this.form = emptyForm()
    },
    async saveCategory() {
      if (!this.canSave) return
      this.saving = true
      try {
        const editing = !!this.form.id
        const payload = {
          code: this.form.code.trim().toUpperCase(),
          name: this.form.name.trim(),
          description: this.form.description.trim() || null,
          active: this.form.active,
        }
        if (editing) await axios.put(`/administrator/ai/knowledge/categories/${this.form.id}`, payload)
        else await axios.post('/administrator/ai/knowledge/categories', payload)
        this.$refs.categoryModal.hide()
        await this.loadCategories()
        toastSuccess(this.$t(editing ? 'aiKnowledge.categories.updated' : 'aiKnowledge.categories.added'))
      } finally {
        this.saving = false
      }
    },
    async deleteCategory(item) {
      const confirmed = await this.$bvModal.msgBoxConfirm(this.$t('aiKnowledge.categories.deleteConfirm', { name: item.name }), {
        title: this.$t('aiKnowledge.categories.confirmDelete'), okVariant: 'danger', okTitle: this.$t('aiKnowledge.categories.delete'), cancelTitle: this.$t('common.cancel'), centered: true,
      })
      if (!confirmed) return
      await axios.delete(`/administrator/ai/knowledge/categories/${item.id}`)
      await this.loadCategories()
      toastSuccess(this.$t('aiKnowledge.categories.deleted'))
    },
  },
}
</script>

<style scoped>
.knowledge-category-page { max-width: 100%; overflow-x: hidden; }
.page-actions { display: flex; gap: 8px; }
.category-table-card { max-width: 100%; overflow: hidden; }
.category-table-card ::v-deep .table-responsive { margin: 0; max-width: 100%; overflow-x: auto; }
.category-description { line-height: 1.4; margin-top: 2px; }
.modal-actions { border-top: 1px solid #e6ebf2; display: flex; gap: 8px; justify-content: flex-end; margin: 16px -16px -16px; padding: 12px 16px; }
@media (max-width: 575.98px) {
  .page-heading { align-items: stretch !important; gap: 12px; }
  .page-heading h4 { flex-basis: 100%; }
  .page-actions { width: 100%; }
  .page-actions .btn { flex: 1; white-space: nowrap; }
}
</style>
