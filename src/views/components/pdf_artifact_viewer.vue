<template>
  <div class="pdf-artifact-viewer">
    <div class="d-flex justify-content-between align-items-center mb-2">
      <span>{{ message }}</span>
      <a v-if="url" :href="url" download="mau-hoa-don.pdf" class="btn btn-sm btn-success"
        >Tải PDF đang xem</a
      >
    </div>
    <b-alert v-if="error" show variant="danger"
      >{{ error }}
      <b-button v-if="retryable" size="sm" variant="outline-danger" @click="retry"
        >Thử lại</b-button
      ></b-alert
    >
    <div v-if="loading" class="text-center py-5"><b-spinner small /> Đang tạo bản in…</div>
    <iframe v-if="url" :src="url" title="Bản in hóa đơn" />
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'PdfArtifactViewer',
  props: { jobId: { type: Number, default: null } },
  data: () => ({
    url: '',
    message: 'Chọn xem trước để tạo bản in',
    error: '',
    loading: false,
    retryable: true,
    timer: null,
    generation: 0
  }),
  watch: {
    jobId: {
      immediate: true,
      handler() {
        this.start()
      }
    }
  },
  beforeDestroy() {
    this.generation++
    clearTimeout(this.timer)
    this.release()
  },
  methods: {
    release() {
      if (this.url) URL.revokeObjectURL(this.url)
      this.url = ''
    },
    start() {
      const generation = ++this.generation
      clearTimeout(this.timer)
      this.release()
      this.error = ''
      this.retryable = true
      this.loading = !!this.jobId
      if (this.jobId) this.poll(generation, Date.now())
    },
    async poll(generation, startedAt) {
      try {
        const { data } = await axios.get(`/template-render-jobs/${this.jobId}`)
        if (generation !== this.generation) return
        if (data.state === 'CANCELLED') {
          this.retryable = false
          throw new Error('Tác vụ tạo PDF đã dừng. Hãy tạo bản xem trước mới.')
        }
        if (data.state === 'FAILED') throw new Error(data.message || 'Không tạo được PDF')
        if (data.state === 'SUCCEEDED') {
          const response = await axios.get(`/template-artifacts/${data.artifactId}/content`, {
            responseType: 'blob'
          })
          if (generation !== this.generation) return
          if (!response.data.type.includes('application/pdf'))
            throw new Error('Phản hồi không phải PDF')
          this.url = URL.createObjectURL(response.data)
          this.message = 'Bản in đã sẵn sàng'
          this.loading = false
          this.$emit('artifact', data.artifactId)
          return
        }
        if (Date.now() - startedAt > 180000)
          throw new Error('Bản in chưa sẵn sàng; vui lòng thử lại')
        this.message = data.state === 'RUNNING' ? 'Đang xử lý bản in' : 'Đang chờ xử lý'
        this.timer = setTimeout(() => this.poll(generation, startedAt), 700)
      } catch (e) {
        if (generation !== this.generation) return
        this.error = e.response?.data?.message || e.message || 'Không tải được bản in'
        this.loading = false
      }
    },
    async retry() {
      try {
        await axios.post(`/template-render-jobs/${this.jobId}/retry`)
        this.start()
      } catch (e) {
        this.error = e.response?.data?.message || 'Không thể thử lại'
      }
    }
  }
}
</script>

<style scoped>
.pdf-artifact-viewer iframe {
  width: 100%;
  height: 780px;
  border: 1px solid #ddd;
  background: #eee;
}
</style>
