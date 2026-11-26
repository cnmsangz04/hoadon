<template>
  <div class="container-fluid py-3 knowledge-test-page">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">{{ $t('aiKnowledge.test.title') }}</h4>
    </div>

    <b-card class="mb-3 shadow-sm filter-card">
      <b-form @submit.prevent="testRetrieval">
        <b-row class="align-items-end">
          <b-col cols="12" lg="6" class="mb-2 mb-lg-0">
            <b-form-group :label="$t('aiKnowledge.test.query')" label-for="knowledge-query" class="mb-0">
              <b-input-group>
                <b-input-group-prepend is-text><i class="fas fa-search text-muted"></i></b-input-group-prepend>
                <b-form-input id="knowledge-query" v-model.trim="form.query"
                              :placeholder="$t('aiKnowledge.test.queryPlaceholder')" />
              </b-input-group>
            </b-form-group>
          </b-col>
          <b-col cols="12" sm="5" lg="2" class="mb-2 mb-lg-0">
            <b-form-group :label="$t('aiKnowledge.test.maxSources')" label-for="knowledge-limit" class="mb-0">
              <b-form-select id="knowledge-limit" v-model.number="form.limit" :options="limitOptions" />
            </b-form-group>
          </b-col>
          <b-col cols="12" sm="7" lg="4" class="test-actions">
            <b-button size="sm" variant="outline-primary" type="submit" :disabled="busy || !hasInput">
              <b-spinner v-if="busy" small class="mr-1" />
              <i v-else class="fas fa-search mr-1"></i>{{ $t('aiKnowledge.test.retrieve') }}
            </b-button>
            <b-button size="sm" variant="primary" type="button" :disabled="busy || !hasInput" @click="askAi">
              <i class="fas fa-robot mr-1"></i>{{ $t('aiKnowledge.test.askGemini') }}
            </b-button>
          </b-col>
        </b-row>
        <div class="retrieval-option">
          <b-form-checkbox v-model="form.externalOnly">{{ $t('aiKnowledge.test.externalOnly') }}</b-form-checkbox>
        </div>
      </b-form>
    </b-card>

    <b-card class="shadow-sm result-card">
      <div class="d-flex align-items-center justify-content-between flex-wrap mb-2 table-toolbar">
        <span class="text-muted small"><i class="fas fa-list-alt mr-1"></i>{{ $t('aiKnowledge.test.retrievedCount', { count: sources.length }) }}</span>
        <span v-if="requestId" class="text-muted small">Request ID: {{ requestId }}</span>
      </div>

      <div v-if="answer" class="ai-answer mb-3">
        <div class="font-weight-bold mb-1"><i class="fas fa-robot text-primary mr-1"></i>{{ $t('aiKnowledge.test.answer') }}</div>
        <div class="answer-content">{{ answer }}</div>
      </div>

      <b-table bordered hover responsive small show-empty class="mb-0 table-modern table-compact"
               :items="sources" :fields="fields" :empty-text="$t('aiKnowledge.test.empty')">
        <template #cell(index)="{ index }">{{ index + 1 }}</template>
        <template #cell(title)="{ item }">
          <strong>{{ item.title || '-' }}</strong>
          <div v-if="item.heading" class="small text-muted">{{ item.heading }}</div>
        </template>
        <template #cell(sourceLocation)="{ item }">{{ item.sourceLocation || '-' }}</template>
        <template #cell(score)="{ item }">
          <b-badge variant="light">{{ Number(item.score || 0).toFixed(3) }}</b-badge>
        </template>
        <template #cell(content)="{ item }"><div class="source-text">{{ item.content || '-' }}</div></template>
      </b-table>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'AdminAiKnowledgeTest',
  data() {
    return {
      busy: false,
      form: { query: '', limit: 5, externalOnly: true },
      sources: [],
      answer: '',
      requestId: '',
    }
  },
  computed: {
    limitOptions() {
      return [3, 5, 8].map(value => ({ value, text: this.$t('aiKnowledge.test.sourceOption', { count: value }) }))
    },
    fields() {
      return [
        { key: 'index', label: '#', thStyle: { width: '48px' }, tdClass: 'text-center' },
        { key: 'title', label: this.$t('aiKnowledge.test.columns.document'), thStyle: { width: '240px' } },
        { key: 'sourceLocation', label: this.$t('aiKnowledge.test.columns.location'), thStyle: { width: '150px' } },
        { key: 'score', label: this.$t('aiKnowledge.test.columns.relevance'), thStyle: { width: '110px' }, tdClass: 'text-center' },
        { key: 'content', label: this.$t('aiKnowledge.test.columns.content') },
      ]
    },
    hasInput() {
      return !!this.form.query.trim()
    },
  },
  methods: {
    resetResult() {
      this.sources = []
      this.answer = ''
      this.requestId = ''
    },
    async testRetrieval() {
      if (!this.hasInput || this.busy) return
      this.busy = true
      this.resetResult()
      try {
        const { data } = await axios.post('/administrator/ai/knowledge/retrieve', {
          query: this.form.query,
          limit: this.form.limit,
          externalOnly: this.form.externalOnly,
        })
        this.sources = data.sources || []
      } finally {
        this.busy = false
      }
    },
    async askAi() {
      if (!this.hasInput || this.busy) return
      this.busy = true
      this.resetResult()
      try {
        const { data } = await axios.post('/administrator/ai/knowledge/ask', { question: this.form.query })
        this.answer = data.answer || ''
        this.requestId = data.requestId || ''
        this.sources = data.sources || []
      } finally {
        this.busy = false
      }
    },
  },
}
</script>

<style scoped>
.knowledge-test-page { max-width: 100%; overflow-x: hidden; }
.test-actions { display: flex; gap: 8px; justify-content: flex-end; }
.retrieval-option { border-top: 1px solid #e6ebf2; margin-top: 14px; padding-top: 12px; }
.result-card { max-width: 100%; overflow: hidden; }
.result-card ::v-deep .table-responsive { margin: 0; max-width: 100%; overflow-x: auto; }
.ai-answer { background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 6px; padding: 12px; }
.answer-content { line-height: 1.6; white-space: pre-wrap; }
.source-text { line-height: 1.5; max-height: 86px; overflow: auto; white-space: pre-wrap; word-break: break-word; }
@media (max-width: 991.98px) {
  .test-actions { justify-content: flex-start; }
}
@media (max-width: 575.98px) {
  .test-actions .btn { flex: 1; white-space: nowrap; }
}
</style>
