<template>
    <div class="container-fluid py-3 form-invoice-create">

        <!-- Thẻ thông tin chính -->
        <div class="card form-panel shadow-sm">
            <div class="card-header form-panel-header">
                <h4 class="card-title mb-0">
                    <i class="fa fa-file-text-o"></i> Thông tin mẫu hóa đơn
                </h4>
            </div>
            <div class="card-body">
                <b-form @submit.prevent="onSubmit">
                    <!-- Tên mẫu -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">
                            Tên mẫu <span class="text-danger">*</span>
                        </label>
                        <div class="col-md-9">
                            <b-form-input
                                v-model="form.name"
                                required
                                placeholder="Nhập tên mẫu hóa đơn"
                            />
                        </div>
                    </div>

                    <!-- Loại hóa đơn & Thuế suất -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">
                            Loại hóa đơn <span class="text-danger">*</span>
                        </label>
                        <div class="col-md-3">
                            <b-form-select
                                v-model.number="form.category"
                                :options="categoryOptions"
                                required
                            />
                        </div>
                        <label class="col-md-2 col-form-label">
                            Thuế suất <span class="text-danger">*</span>
                        </label>
                        <div class="col-md-4">
                            <b-form-select
                                v-model.number="form.type"
                                :options="typeOptions"
                                required
                            />
                        </div>
                    </div>

                    <!-- Hình thức & Trạng thái -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">Hình thức</label>
                        <div class="col-md-3">
                            <b-form-select
                                v-model.number="form.have_code"
                                :options="haveCodeOptions"
                            />
                        </div>
                        <label class="col-md-2 col-form-label">Trạng thái</label>
                        <div class="col-md-4">
                            <b-form-select
                                v-model.number="form.status"
                                :options="statusOptions"
                            />
                        </div>
                    </div>

                    <hr class="my-4" />

                    <!-- Tập tin mẫu -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">Tập tin mẫu (XSLT)</label>
                        <div class="col-md-9">
                            <input
                                ref="fileInput"
                                type="file"
                                accept=".xslt,.xml,text/xml"
                                @change="onFileChange"
                                class="d-none"
                            />
                            <div class="file-upload-block">
                                <b-button variant="outline-success" size="sm" @click="$refs.fileInput.click()">
                                    <i class="fa fa-file-code-o"></i> Chọn file
                                </b-button>
                                <span class="file-name-text ml-2">{{ fileName || 'Chưa chọn file' }}</span>
                                <b-button
                                    v-if="fileName"
                                    variant="link"
                                    size="sm"
                                    class="text-danger ml-2"
                                    @click.prevent="clearFile"
                                >
                                    <i class="fa fa-times"></i> Xóa
                                </b-button>
                            </div>
                            <div class="file-preview-box mt-2" v-if="filePreviewUrl || fileName">
                                <template v-if="filePreviewIsImage">
                                    <img
                                        :src="filePreviewUrl"
                                        alt="Xem trước"
                                        class="preview-img"
                                        @click.prevent="previewFile('file')"
                                    />
                                </template>
                                <template v-else>
                                    <div class="file-info-box">
                                        <i class="fa fa-file-text-o fa-2x text-muted"></i>
                                        <div class="ml-3">
                                            <div class="filename-display">{{ fileName }}</div>
                                            <a href="#" @click.prevent="previewFile('file')" class="text-primary small">
                                                <i class="fa fa-eye"></i> Xem nội dung
                                            </a>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </div>

                    <!-- Ảnh mẫu -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">Ảnh mẫu</label>
                        <div class="col-md-9">
                            <input
                                ref="photoInput"
                                type="file"
                                accept=".png,.jpg,.jpeg"
                                @change="onPhotoChange"
                                class="d-none"
                            />
                            <div class="file-upload-block">
                                <b-button variant="outline-success" size="sm" @click="$refs.photoInput.click()">
                                    <i class="fa fa-image"></i> Chọn ảnh
                                </b-button>
                                <span class="file-name-text ml-2">{{ photoName || 'Chưa chọn ảnh' }}</span>
                                <b-button
                                    v-if="photoName"
                                    variant="link"
                                    size="sm"
                                    class="text-danger ml-2"
                                    @click.prevent="clearPhoto"
                                >
                                    <i class="fa fa-times"></i> Xóa
                                </b-button>
                            </div>
                            <div class="file-preview-box mt-2" v-if="photoPreviewUrl">
                                <img
                                    :src="photoPreviewUrl"
                                    alt="Ảnh mẫu"
                                    class="preview-img"
                                    @click.prevent="previewFile('photo')"
                                />
                            </div>
                        </div>
                    </div>

                    <hr class="my-4" />

                    <!-- Nút hành động -->
                    <div class="form-group row mb-0">
                        <div class="col-md-12 text-right">
                            <b-button variant="secondary" size="sm" @click="$router.back()">
                                <i class="fa fa-arrow-left"></i> Quay lại
                            </b-button>
                            <b-button
                                type="submit"
                                :disabled="isSaving"
                                variant="primary"
                                class="ml-2"
                                size="sm"
                            >
                                <i class="fa fa-save"></i>
                                {{ isEdit ? (isSaving ? 'Đang cập nhật...' : 'Cập nhật') : (isSaving ? 'Đang tạo...' : 'Lưu') }}
                            </b-button>
                        </div>
                    </div>
                </b-form>
            </div>
        </div>

        <!-- Hộp thoại xem trước -->
        <b-modal ref="filePreviewModal" hide-footer :title="filePreviewModalTitle || 'Xem file'" size="lg">
            <div v-if="filePreviewModalLoading" class="text-center py-4">
                <i class="fa fa-spinner fa-spin fa-2x"></i>
                <p class="mt-2">Đang tải...</p>
            </div>
            <div v-else>
                <div v-if="!filePreviewModalIsText" class="text-center">
                    <img :src="filePreviewModalImg" alt="Xem trước" class="modal-preview-img" />
                </div>
                <div v-else>
                    <pre class="preview-code">{{ filePreviewModalContent }}</pre>
                </div>
            </div>
        </b-modal>
    </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
    name: 'AdminFormInvoiceCreate',
    data() {
        return {
            isEdit: false,
            id: null,
            form: {
                name: '',
                category: 1, // mặc định: Hóa đơn GTGT
                type: 1,     // mặc định: Một thuế suất
                status: 0,
                have_code: 1, // mặc định: Có mã
                file: null,
                photo: null
            },
            fileInputObj: null,
            fileName: '',
            filePreviewUrl: '',
            filePreviewIsImage: false,
            filePreviewObjectUrl: false,
            photoInputObj: null,
            photoName: '',
            photoPreviewUrl: '',
            photoPreviewObjectUrl: false,
            // Trạng thái hộp thoại xem trước
            filePreviewModalContent: '',
            filePreviewModalLoading: false,
            filePreviewModalTitle: '',
            filePreviewModalIsText: false,
            filePreviewModalImg: '',
            categoryOptions: [
                { value: 1, text: 'Hóa đơn giá trị gia tăng' },
                { value: 2, text: 'Hóa đơn bán hàng' }
            ],
            typeOptions: [
                { value: 1, text: 'Một thuế suất' },
                { value: 2, text: 'Nhiều thuế suất' }
            ],
            statusOptions: [
                { value: 1, text: 'Kích hoạt' },
                { value: 0, text: 'Chưa kích hoạt' }
            ],
            haveCodeOptions: [
                { value: 1, text: 'Có mã' },
                { value: 0, text: 'Không mã' }
            ],
            isSaving: false
        }
    },
    created() {
        const id = this.$route.params.id
        if (id) this.load(id)
    },
    methods: {
        reload() { 
            if (this.isEdit && this.id) this.load(this.id) 
        },
        onFileChange(e) {
            const f = e.target.files && e.target.files[0]
            if (!f) { this.fileInputObj = null; this.fileName = ''; this.filePreviewUrl = ''; this.filePreviewIsImage = false; this.filePreviewObjectUrl = false; return }
            this.fileInputObj = f; this.fileName = f.name
            // Nhận diện file ảnh để hiển thị xem trước trực tiếp
            const lower = (f.name || '').toLowerCase()
            const isImage = f.type && f.type.startsWith('image/') || /\.(png|jpe?g|gif|webp)$/i.test(lower)
            if (isImage) {
                if (this.filePreviewObjectUrl && this.filePreviewUrl) URL.revokeObjectURL(this.filePreviewUrl)
                this.filePreviewUrl = URL.createObjectURL(f)
                this.filePreviewIsImage = true
                this.filePreviewObjectUrl = true
            } else {
                // File không phải ảnh, thường là xslt/xml: không xem trước ảnh và xóa object url nếu có
                if (this.filePreviewObjectUrl && this.filePreviewUrl) { URL.revokeObjectURL(this.filePreviewUrl) }
                this.filePreviewUrl = ''
                this.filePreviewIsImage = false
                this.filePreviewObjectUrl = false
            }
        },
        onPhotoChange(e) {
            const f = e.target.files && e.target.files[0]
            if (!f) { this.photoInputObj = null; this.photoName = ''; this.photoPreviewUrl = ''; this.photoPreviewObjectUrl = false; return }
            this.photoInputObj = f; this.photoName = f.name
            if (this.photoPreviewObjectUrl && this.photoPreviewUrl) URL.revokeObjectURL(this.photoPreviewUrl)
            this.photoPreviewUrl = URL.createObjectURL(f)
            this.photoPreviewObjectUrl = true
        },
        clearFile() {
            if (this.filePreviewObjectUrl && this.filePreviewUrl) { URL.revokeObjectURL(this.filePreviewUrl) }
            this.fileInputObj = null; this.fileName = ''; this.filePreviewUrl = ''; this.filePreviewIsImage = false; this.filePreviewObjectUrl = false; this.$refs.fileInput.value = null
        },
        clearPhoto() {
            if (this.photoPreviewObjectUrl && this.photoPreviewUrl) { URL.revokeObjectURL(this.photoPreviewUrl) }
            this.photoInputObj = null; this.photoName = ''; this.photoPreviewUrl = ''; this.photoPreviewObjectUrl = false; this.$refs.photoInput.value = null
        },
        async load(id) {
            try {
                const { data } = await axios.get(`/administrator/form-invoices/${id}`)
                this.isEdit = true
                this.id = id
                this.form = {
                    ...this.form,
                    name: data.name,
                    category: data.category != null ? data.category : 1,
                    type: data.type != null ? data.type : 1,
                    status: data.status != null ? data.status : 0,
                    have_code: data.haveCode != null ? data.haveCode : (data.have_code != null ? data.have_code : 1)
                }
                this.fileName = data.file ? (data.file.split('/').pop() || data.file) : ''
                this.photoName = data.photo ? (data.photo.split('/').pop() || data.photo) : ''
                if (data.photo) {
                    this.photoPreviewUrl = data.photo
                    this.photoPreviewObjectUrl = false
                }
                if (data.file) {
                    const lower = (data.file || '').toLowerCase()
                    if (/\.(png|jpe?g|gif|webp)$/i.test(lower)) {
                        this.filePreviewUrl = data.file
                        this.filePreviewIsImage = true
                        this.filePreviewObjectUrl = false
                    } else {
                        this.filePreviewUrl = data.file
                        this.filePreviewIsImage = false
                        this.filePreviewObjectUrl = false
                    }
                }
            } catch (e) {
                console.error('Lỗi tải mẫu hóa đơn:', e)
            }
        },
        previewFile(source) {
            // Mặc định xem file mẫu nếu không truyền nguồn cụ thể
            source = source || 'file';
            this.filePreviewModalContent = '';
            this.filePreviewModalLoading = true;
            this.filePreviewModalIsText = false;
            this.filePreviewModalImg = '';
            // Ưu tiên hiển thị tên file ngắn gọn thay vì toàn bộ đường dẫn
            this.filePreviewModalTitle = (source === 'photo' ? this.photoName : this.fileName) || this.fileName || this.photoName || 'Xem file';

            if (source === 'file') {
                // Nếu đã chọn file local thì ưu tiên file đó (ảnh thì hiển thị ảnh, còn lại đọc dạng text)
                if (this.fileInputObj) {
                    if (this.filePreviewIsImage && this.filePreviewUrl) {
                        this.filePreviewModalLoading = false;
                        this.filePreviewModalIsText = false;
                        this.filePreviewModalImg = this.filePreviewUrl;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                        return;
                    }
                    const reader = new FileReader();
                    reader.onload = (ev) => {
                        this.filePreviewModalContent = ev.target.result;
                        this.filePreviewModalIsText = true;
                        this.filePreviewModalLoading = false;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    };
                    reader.onerror = () => {
                        this.filePreviewModalContent = 'Không thể đọc file';
                        this.filePreviewModalIsText = true;
                        this.filePreviewModalLoading = false;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    };
                    reader.readAsText(this.fileInputObj);
                    return;
                }

                // Nếu có URL file đã lưu từ xa thì xử lý tiếp (ảnh thì hiển thị ảnh, còn lại fetch dạng text)
                if (this.filePreviewUrl) {
                    if (this.filePreviewIsImage) {
                        this.filePreviewModalLoading = false;
                        this.filePreviewModalIsText = false;
                        this.filePreviewModalImg = this.filePreviewUrl;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                        return;
                    }
                    axios.get(this.filePreviewUrl, { responseType: 'text' }).then(res => {
                        this.filePreviewModalContent = res.data;
                        this.filePreviewModalIsText = true;
                        this.filePreviewModalLoading = false;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    }).catch(err => {
                        this.filePreviewModalContent = 'Không thể tải file để xem';
                        this.filePreviewModalIsText = true;
                        this.filePreviewModalLoading = false;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    });
                    return;
                }

                // Nếu không có file thì dự phòng bằng ảnh xem trước
                if (this.photoPreviewUrl) {
                    this.filePreviewModalLoading = false;
                    this.filePreviewModalIsText = false;
                    this.filePreviewModalImg = this.photoPreviewUrl;
                    this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    return;
                }
            } else if (source === 'photo') {
                // Nếu người dùng chọn ảnh mẫu thì hiển thị ảnh trước
                if (this.photoPreviewUrl) {
                    this.filePreviewModalLoading = false;
                    this.filePreviewModalIsText = false;
                    this.filePreviewModalImg = this.photoPreviewUrl;
                    this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    return;
                }
                // Nếu chưa có ảnh mẫu thì dùng lại luồng xem file mẫu
                if (this.fileInputObj || this.filePreviewUrl) {
                    this.previewFile('file');
                    return;
                }
            }

            // Trường hợp cuối cùng khi không có nội dung để xem
            this.filePreviewModalContent = 'Không có file để xem';
            this.filePreviewModalIsText = true;
            this.filePreviewModalLoading = false;
            this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
        },
        async onSubmit() {
            if (!this.form.name || !this.form.name.trim()) {
                this.$bvToast && this.$bvToast.toast('Tên mẫu là bắt buộc', { 
                    title: 'Lỗi', 
                    variant: 'danger', 
                    solid: true 
                })
                return
            }

            if (this.isSaving) return
            this.isSaving = true

            const fd = new FormData()
            if (this.id) fd.append('id', this.id)
            fd.append('name', this.form.name)
            if (this.form.category != null) fd.append('category', this.form.category)
            if (this.form.type != null) fd.append('type', this.form.type)
            fd.append('status', this.form.status)
            fd.append('have_code', this.form.have_code)
            // không gửi companyId, serial; backend tự thiết lập
            if (this.fileInputObj) fd.append('file', this.fileInputObj)
            if (this.photoInputObj) fd.append('photo', this.photoInputObj)

            try {
                await axios.post('/administrator/form-invoices/saveOrUpdate', fd, { 
                    headers: { 'Content-Type': 'multipart/form-data' } 
                })
                this.$bvToast && this.$bvToast.toast('Lưu thành công', { 
                    title: 'Thành công', 
                    variant: 'success', 
                    solid: true 
                })
                this.$router.push({ name: 'admin-form-invoice-list' })
            } catch (e) {
                const msg = e?.response?.data?.message || 'Lỗi lưu mẫu'
                this.$bvToast && this.$bvToast.toast(msg, { 
                    title: 'Lỗi', 
                    variant: 'danger', 
                    solid: true 
                })
            } finally {
                this.isSaving = false
            }
        }
    }
}
</script>

<style scoped>
.form-invoice-create {
    font-size: 13px;
}

.form-panel {
    border: 1px solid #e6ebf2;
    border-radius: 8px;
    overflow: hidden;
}

.form-panel-header {
    align-items: center;
    background: #f8fafc;
    border-bottom: 1px solid #e6ebf2;
    display: flex;
    min-height: 48px;
    padding: 12px 16px;
}

.card-title {
    color: #1f2937;
    font-size: 16px;
    font-weight: 700;
}

.card-title i {
    color: #2563eb;
    margin-right: 8px;
}

.card-body {
    padding: 18px;
}

.form-group.row {
    margin-bottom: 16px;
}

.col-form-label {
    color: #334155;
    font-weight: 600;
    padding-top: 7px;
    text-align: right;
}

hr {
    border-top: 1px solid #e6ebf2;
}

.file-upload-block {
    align-items: center;
    background: #f8fafc;
    border: 1px dashed #d8dee8;
    border-radius: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    min-height: 44px;
    padding: 8px;
}

.file-name-text {
    color: #64748b;
    flex: 1 1 220px;
    font-size: 13px;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.file-preview-box {
    background: #fff;
    border: 1px solid #e6ebf2;
    border-radius: 8px;
    padding: 10px;
}

.preview-img {
    border: 1px solid #e6ebf2;
    border-radius: 6px;
    cursor: pointer;
    display: block;
    max-height: 150px;
    max-width: 220px;
    object-fit: contain;
}

.file-info-box {
    align-items: center;
    display: flex;
    min-width: 0;
    padding: 4px;
}

.filename-display {
    color: #1f2937;
    font-weight: 600;
    margin-bottom: 2px;
    overflow-wrap: anywhere;
}

.preview-code {
    background-color: #0f172a;
    border: 1px solid #1e293b;
    border-radius: 8px;
    color: #e2e8f0;
    font-family: "Courier New", Courier, monospace;
    font-size: 13px;
    max-height: 500px;
    overflow: auto;
    padding: 14px;
    white-space: pre-wrap;
}

.modal-preview-img {
    display: block;
    margin: 0 auto;
    max-height: 70vh;
    max-width: 100%;
    object-fit: contain;
}

@media (max-width: 768px) {
    .col-form-label {
        padding-top: 0;
        text-align: left;
    }

    .card-body {
        padding: 14px;
    }
}
</style>
