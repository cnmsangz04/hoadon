<template>
  <div class="container-fluid py-3 register-invoice-create">
    <!-- Tiêu đề -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">Tờ Khai Hóa Đơn Điện Tử</h4>
        <b-badge class="ml-2" variant="light">{{ frmData.action === 'update' ? 'Cập nhật' : 'Tạo mới' }}</b-badge>
      </div>
    </div>

    <b-card class="shadow-sm">
      <!-- Khối: Thông tin tờ khai -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">Thông tin tờ khai</b-card-header>
        <b-card-body>
          <!-- Hình thức tờ khai -->
          <b-form-group label="Hình thức tờ khai" label-class="font-weight-bold">
            <b-form-radio-group v-model.number="frmData.declaration_type" :options="declarationTypeOptions" switches />
          </b-form-group>

          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Mẫu số tờ khai" label-class="font-weight-bold">
                <div class="form-control-plaintext font-weight-bold">{{ frmData.form_pattern }}</div>
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Ngày lập" label-class="font-weight-bold">
                <b-form-datepicker v-model="frmData.declaration_date" :date-format-options="dateFmt" locale="vi" />
              </b-form-group>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- Khối: Thông tin người nộp thuế -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">Thông tin người nộp thuế</b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Tên người nộp thuế:</span> <span>{{ company.name }}</span></div>
            </b-col>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Mã số thuế:</span> <span>{{ company.tax_code }}</span></div>
            </b-col>
          </b-row>

          <b-row>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Cơ quan thuế quản lý:</span> <span>{{ frmData.tax_authority_name }}</span></div>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- Khối: Người đại diện pháp luật -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">Người đại diện pháp luật</b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Tên người đại diện pháp luật:</span> <span>{{ legalRep.fullname }}</span></div>
            </b-col>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Điện thoại người đại diện pháp luật:</span> <span>{{ legalRep.phone }}</span></div>
            </b-col>
          </b-row>

          <b-row>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Căn cước công dân:</span> <span>{{ legalRep.citizen_id }}</span></div>
            </b-col>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Số hộ chiếu:</span> <span>{{ legalRep.passport_no }}</span></div>
            </b-col>
          </b-row>

          <b-row>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Ngày sinh:</span> <span>{{ formatDate(legalRep.date_of_birth) }}</span></div>
            </b-col>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Giới tính:</span> <span>{{ genderText(legalRep.gender) }}</span></div>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- Khối: Thông tin liên hệ -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">Thông tin liên hệ</b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Địa chỉ liên hệ:</span> <span>{{ company.address }}</span></div>
            </b-col>
            <b-col cols="12" md="6">
              <div class="mb-2"><span class="font-weight-bold">Thư điện tử:</span> <span>{{ company.email }}</span></div>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- Thuyết minh -->
      <b-alert show variant="light" class="mt-2">
        Theo Nghị định số 119/2018/NĐ-CP ngày 12 tháng 09 năm 2018 của Chính phủ, chúng tôi/tôi thuộc đối tượng sử dụng hóa đơn điện tử. Chúng tôi/tôi đăng ký/thay đổi thông tin đã đăng ký với cơ quan thuế về việc sử dụng hóa đơn điện tử như sau:
      </b-alert>

      <!-- 1. Hình thức hóa đơn -->
      <b-row class="pt-2 pb-2">
        <b-col cols="12">
          <label class="font-weight-bold">1. Hình thức hóa đơn</label>
          <b-form-checkbox-group class="ml-3" stacked v-model="frmData.invoice_forms">
            <b-form-checkbox class="mb-2" value="CMa">Hóa đơn có mã</b-form-checkbox>
            <b-form-checkbox class="mb-2" value="KCMa">Hóa đơn không có mã</b-form-checkbox>
            <b-form-checkbox value="CMTMTTien">Hóa đơn có mã từ máy tính tiền</b-form-checkbox>
          </b-form-checkbox-group>
        </b-col>
      </b-row>

      <!-- 2. Hình thức gửi dữ liệu hóa đơn điện tử -->
      <b-row class="pt-2 pb-2">
        <b-col cols="12">
          <label class="font-weight-bold">2. Hình thức gửi dữ liệu hóa đơn điện tử</label>
          <div class="pl-3">
            <div class="pb-2">
              <b-form-checkbox v-model="sendToggleA">
                <b>a.</b> Trương hợp sử dụng hóa đơn điện tử có mã không phải trả tiền dịch vụ theo khoản 1 Điều 14 của Nghị Định:
              </b-form-checkbox>
            </div>
            <b-form-checkbox-group class="pl-3" stacked v-model="frmData.send_methods_a">
              <div class="pl-2">
                <b-form-checkbox class="mb-2" value="NNTDBKKhan">Doanh nghiệp nhỏ và vừa, hợp tác xã, hộ, cá nhân kinh doanh tại địa bàn có điều kiện kinh tế xã hội khó khăn, địa bàn có điều kiện kinh tế xã hội đặc biệt khó khăn</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="NNTKTDNUBND">Doanh nghiệp nhỏ và vừa khác theo đề nghị của Ủy ban nhân dân tỉnh, thành phố trực thuộc trung ương gửi Bộ Tài chính trừ doanh nghiệp hoạt động tại các khu kinh tế, khu công nghiệp, khu công nghệ cao</b-form-checkbox>
              </div>
            </b-form-checkbox-group>
            <div class="pb-2">
              <b-form-checkbox v-model="sendToggleB">
                <b>b.</b> Trương hợp sử dụng hóa đơn điện tử không có mã của cơ quan thuế:
              </b-form-checkbox>
            </div>
            <b-form-checkbox-group class="pl-3" stacked v-model="frmData.send_methods_b">
              <div class="pl-2">
                <b-form-checkbox class="mb-2" value="CDLTTDCQT">Chuyển dữ liệu hóa đơn điện tử trực tiếp đến cơ quan thuế (điểm b1, khoản 3, Điều 22 của Nghị định)</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="CDLQTCTN">Thông qua tổ chức cung cấp dịch vụ hóa đơn điện tử (điểm b2, khoản 3, Điều 22 của Nghị định)</b-form-checkbox>
              </div>
            </b-form-checkbox-group>
            <div class="pb-2">
              <b-form-checkbox v-model="sendToggleC">
                <b>c.</b> Cơ quan thuế hoặc cơ quan được giao nhiệm vụ tổ chức, xử lý tài sản công theo quy định pháp luật về quản lý, sử dụng tài sản công (khoản 11 điều 1 Nghị định 70)
              </b-form-checkbox>
            </div>
          </div>
        </b-col>
      </b-row>

      <!-- 3. Phương thức chuyển dữ liệu hóa đơn điện tử -->
      <b-row class="pt-2 pb-2">
        <b-col cols="12">
          <label class="font-weight-bold">3. Phương thức chuyển dữ liệu hóa đơn điện tử</label>
          <b-form-checkbox-group class="ml-3" stacked v-model="frmData.transfer_methods">
            <b-form-checkbox class="mb-2" value="CDDu">Chuyển đầy đủ nội dung từng hóa đơn</b-form-checkbox>
            <b-form-checkbox value="CBTHop">Chuyển theo bảng tổng hợp dữ liệu hóa đơn điện tử (điểm a1, khoản 3, Điều 22 của Nghị định)</b-form-checkbox>
          </b-form-checkbox-group>
        </b-col>
      </b-row>

      <!-- 4. Loại hóa đơn sử dụng -->
      <b-row class="pt-2 pb-2">
        <b-col cols="12">
          <label class="font-weight-bold">4. Loại hóa đơn sử dụng</label>
          <b-form-checkbox-group class="ml-3" stacked v-model="frmData.invoice_types">
            <b-row>
              <b-col cols="12" md="6">
                <b-form-checkbox class="mb-2" value="HDGTGT">Hóa đơn GTGT</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="HDGTGT_BienLai">Hóa đơn GTGT tích hợp biên lai thu thuế, phí, lệ phí</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="HDBHang">Hóa đơn bán hàng</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="HDBHang_BienLai">Hóa đơn bán hàng tích hợp biên lai thu thuế, phí, lệ phí</b-form-checkbox>
              </b-col>
              <b-col cols="12" md="6">
                <b-form-checkbox class="mb-2" value="HDThuongMai">Hóa đơn thương mại</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="HDBTSCong">Hóa đơn bán tài sản công</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="HDBHDTQGia">Hóa đơn bán hàng dự trữ quốc gia</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="HDKhac">Hóa đơn khác</b-form-checkbox>
                <b-form-checkbox class="mb-2" value="CTu">Các chứng từ được in, phát hành, sử dụng và quản lý như hóa đơn</b-form-checkbox>
              </b-col>
            </b-row>
          </b-form-checkbox-group>
        </b-col>
      </b-row>

      <!-- 5. Danh sách chứng thư số sử dụng -->
      <b-row class="pt-2 pb-2">
        <b-col cols="12">
          <label class="font-weight-bold">5. Danh sách chứng thư số sử dụng</label>
          <b-button type="button" variant="link" class="text-decoration-none float-right" @click="showSignatureModal(false)">
            <i class="fas fa-plus"></i> Thêm chữ ký số
          </b-button>
          <div>
            <b-table-simple bordered show-empty empty-text="Không có dữ liệu">
              <b-thead>
                <b-tr>
                  <b-th style="width: 5%" rowspan="2" class="text-center align-middle">STT</b-th>
                  <b-th style="width: 20%" rowspan="2" class="text-center align-middle">Tên tổ chức cơ quan chứng thực/cấp/công nhận chữ ký số, chữ ký điện tử</b-th>
                  <b-th rowspan="2" class="text-center align-middle">Số sê-ri chứng thư</b-th>
                  <b-th style="width: 20%" colspan="2" class="text-center align-middle">Thời gian sử dụng chứng thư số</b-th>
                  <b-th style="width: 15%" rowspan="2" class="text-center align-middle">Hình thức đăng ký (Thêm mới, gia hạn, ngừng sử dụng)</b-th>
                  <b-th style="width: 100px" rowspan="2" class="text-center align-middle">Thao tác</b-th>
                </b-tr>
                <b-tr>
                  <b-th class="text-center align-middle">Từ ngày</b-th>
                  <b-th class="text-center align-middle">Đến ngày</b-th>
                </b-tr>
              </b-thead>
              <b-tbody>
                <b-tr v-for="(value, index) in frmData.digital_certificates" :key="index">
                  <b-td class="text-center">{{ index + 1 }}</b-td>
                  <b-td>{{ value.orgName }}</b-td>
                  <b-td>{{ value.serialNo }}</b-td>
                  <b-td class="text-center">{{ formatDate(value.signFromDate) }}</b-td>
                  <b-td class="text-center">{{ formatDate(value.signToDate) }}</b-td>
                  <b-td class="text-center">{{ getNameRegMethod(value.sigRegMethod) }}</b-td>
                  <b-td class="text-center">
                    <button type="button" @click="showSignatureModal(index)" class="btn btn-custom"><i class="far fa-edit"></i></button>
                    <button type="button" @click="onDeleteSignature(index)" class="btn btn-custom"><i class="far fa-trash-alt"></i></button>
                  </b-td>
                </b-tr>
                <b-tr v-if="!frmData.digital_certificates || frmData.digital_certificates.length < 1">
                  <b-td colspan="7" class="text-center">Không có dữ liệu</b-td>
                </b-tr>
              </b-tbody>
            </b-table-simple>
          </div>
        </b-col>
      </b-row>

      <!-- Cam kết & nơi lập, ngày hiệu lực -->
      <b-row class="pt-2 pb-2">
        <b-col cols="12">
          <p>
            Chúng tôi cam kết hoàn toàn chịu trách nhiệm trước pháp luật về tính chính xác, trung thực của nội dung nêu trên và thực hiện theo đúng quy định của pháp luật./.
          </p>
        </b-col>
      </b-row>

      <b-row class="pt-2 pb-2">
        <b-col cols="12" md="6">
          <b-form-group label="Nơi lập" label-class="font-weight-bold">
            <v-select
              id="create_place"
              v-model="frmData.create_place"
              :options="app.provinces"
              label="name"
              :reduce="v => v.id"
              placeholder="Chọn nơi lập"
              append-to-body />
          </b-form-group>
        </b-col>
        <b-col cols="12" md="6" class="text-center">
          <p>{{ selectedPlaceName }}, ngày {{ formatDate(new Date()) }}</p>
          <p class="text-uppercase font-weight-bold">Người Nộp thuế</p>
          <p><i>(Chữ ký số, chữ ký điện tử người nộp thuế)</i></p>
          <b-card class="text-center w-75 m-auto bg-light" :border-variant="frmData.action === 'update' ? 'success' : ''">
            <div class="text-danger" v-if="frmData.action === 'update' && frmData.signature">
              <p class="font-weight-bold pb-1 m-0">Signature Valid <i class="fas fa-check text-success"></i></p>
              <p class="font-weight-bold pb-1 m-0">Ký bởi: {{ frmData.signature.name }}</p>
              <p class="font-weight-bold m-0">Ngày ký: {{ formatDate(frmData.date_sign) }}</p>
            </div>
            <div v-else>
              <i class="fas fa-signature"></i>
              <div class="d-inline-block" v-if="frmData.action === 'update'">
                <b-button v-if="!btnSignature" class="text-decoration-none" @click="onSignature" variant="link">Thực hiện ký số</b-button>
                <b-button class="text-decoration-none" variant="link" v-else>Đang thực hiện...</b-button>
              </div>
            </div>
          </b-card>
        </b-col>
      </b-row>

      <!-- Thanh thao tác dưới -->
      <div class="action-bar mt-3 pt-3">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <b-button size="sm" variant="outline-secondary" class="btn-back" @click="goBack">
              <i class="fas fa-arrow-left"></i> Quay lại
            </b-button>
          </div>
          <div>
            <!-- Chỉ hiển thị Cập nhật nếu status = 0 -->
            <b-button v-if="!btnLoading && (frmData.action === 'create' || frmData.action === 'update' && Number(frmData.status) === 0)" size="sm" variant="primary" class="btn-save" @click="onSubmit">
              <i class="fas fa-save"></i> {{ frmData.action === 'create' ? 'Lưu' : 'Cập nhật' }}
            </b-button>
            <b-button v-else-if="btnLoading" size="sm" class="btn btn-default" disabled>
              <b-spinner small type="grow"></b-spinner>
              Đang lưu...
            </b-button>
            <!-- Chỉ hiển thị Gửi CQT nếu status = 1 -->
            <b-button type="button" size="sm" class="ml-2" @click="sendData" v-if="frmData.action === 'update' && Number(frmData.status) === 1">Gửi CQT</b-button>
          </div>
        </div>
      </div>
    </b-card>

    <!-- Hộp thoại chữ ký số -->
    <b-modal ref="modalSignature" no-close-on-esc no-close-on-backdrop hide-header-close header-bg-variant="light" @hidden="onSignatureModalHidden">
      <template #modal-title>
        <div class="modal-title">{{ signatureModal.index !== false ? 'Cập nhật chữ ký số' : 'Thêm chữ ký số' }}</div>
      </template>
      <b-form>
        <b-row>
          <b-col cols="12">
            <b-form-group label="Tên tổ chức cơ quan chứng thực/cấp/công nhận chữ ký số, chữ ký điện tử" label-for="orgName">
              <b-form-input id="orgName" type="text" v-model.trim="signatureModal.orgName" />
            </b-form-group>
          </b-col>
        </b-row>
        <b-row>
          <b-col cols="12">
            <b-form-group label="Số sê-ri chứng thư" label-for="serialNo">
              <b-form-input id="serialNo" type="text" v-model.trim="signatureModal.serialNo" />
            </b-form-group>
          </b-col>
        </b-row>
        <b-row>
          <b-col cols="12" md="6">
            <b-form-group label="Thời gian từ ngày" label-for="signFromDate">
              <b-form-datepicker id="signFromDate" class="mb-2" size="sm" v-model="signatureModal.signFromDate" :date-format-options="dateFmt" locale="vi" />
            </b-form-group>
          </b-col>
          <b-col cols="12" md="6">
            <b-form-group label="Thời gian đến ngày" label-for="signToDate">
              <b-form-datepicker id="signToDate" class="mb-2" size="sm" v-model="signatureModal.signToDate" :date-format-options="dateFmt" locale="vi" />
            </b-form-group>
          </b-col>
        </b-row>
        <b-row>
          <b-col cols="12">
            <b-form-group label="Hình thức đăng ký (Thêm mới, gia hạn, ngừng sử dụng)" label-for="sigRegMethod">
              <v-select id="sigRegMethod" :options="options.sigRegMethod" v-model="signatureModal.sigRegMethod" :reduce="(v) => v.code" />
            </b-form-group>
          </b-col>
        </b-row>
      </b-form>
      <template #modal-footer>
        <div class="w-100">
          <div class="float-right">
            <b-button size="sm" variant="light" @click="closeSignatureModal">Hủy</b-button>
            <b-button type="button" size="sm" class="btn btn-default" @click="onSubmitSignature">{{ signatureModal.index !== false ? 'Cập nhật' : 'Thêm' }}</b-button>
          </div>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import vSelect from 'vue-select'
import 'vue-select/dist/vue-select.css'

export default {
  name: 'RegisterInvoiceCreate',
  components: { 'v-select': vSelect },
  data() {
    const today = new Date()
    const isoToday = today.toISOString().substring(0, 10)
    return {
      btnLoading: false,
      btnSignature: false,
      sendToggleA: false,
      sendToggleB: false,
      sendToggleC: false,
      dateFmt: { year: 'numeric', month: 'numeric', day: 'numeric' },
      app: { provinces: [] },
      company: { address: '', email: '', name: '', tax_code: '' },
      legalRep: { fullname: '', phone: '', citizen_id: '', passport_no: '', date_of_birth: null, gender: null },
      options: {
        sigRegMethod: [
          { code: 1, label: 'Thêm mới' },
          { code: 2, label: 'Gia hạn' },
          { code: 3, label: 'Ngừng sử dụng' }
        ]
      },
      frmData: {
        action: 'create', // or 'update'
        declaration_type: 1,
        form_pattern: '01/ĐKTĐ-HĐĐT',
        declaration_date: isoToday,
        tax_authority_name: '',
        tax_authority_code: '',
        invoice_forms: [],
        send_methods_a: [],
        send_methods_b: [],
        transfer_methods: [],
        invoice_types: [],
        digital_certificates: [],
        create_place: '',
        signature: null,
        signed_xml: null,
        date_sign: null,
        status: 0
      },
      signatureModal: {
        index: false,
        orgName: '',
        serialNo: '',
        signFromDate: null,
        signToDate: null,
        sigRegMethod: 1 // default: Thêm mới
      },
      declarationTypeOptions: [
        { value: 1, text: 'Đăng ký mới' },
        { value: 2, text: 'Thay đổi thông tin' }
      ],
      genderOptions: [
        { value: 'M', text: 'Nam' },
        { value: 'F', text: 'Nữ' },
        { value: 'O', text: 'Khác' }
      ]
    }
  },
  computed: {
    selectedPlaceName() {
      const p = this.app.provinces.find(x => x.id === this.frmData.create_place)
      return p ? p.name : ''
    }
  },
  created() {
    this.bootstrap()
  },
  watch: {
    // Parent A toggled: apply to children
    sendToggleA(val) {
      const allA = [
        'NNTDBKKhan',
        'NNTKTDNUBND'
      ]
      if (val) {
        // check all children
        this.frmData.send_methods_a = Array.from(new Set([...(this.frmData.send_methods_a||[]), ...allA]))
      } else {
        // clear children
        this.frmData.send_methods_a = []
      }
    },
    // Children A changed: reflect parent
    'frmData.send_methods_a'(val) {
      const hasAny = Array.isArray(val) && val.length > 0
      this.sendToggleA = hasAny
    },
    // Parent B toggled: apply to children
    sendToggleB(val) {
      const allB = [
        'CDLTTDCQT',
        'CDLQTCTN'
      ]
      if (val) {
        this.frmData.send_methods_b = Array.from(new Set([...(this.frmData.send_methods_b||[]), ...allB]))
      } else {
        this.frmData.send_methods_b = []
      }
    },
    // Children B changed: reflect parent
    'frmData.send_methods_b'(val) {
      const hasAny = Array.isArray(val) && val.length > 0
      this.sendToggleB = hasAny
    },
    // C has no children; keep as-is, but ensure boolean
    sendToggleC(val) {
      this.sendToggleC = !!val
    },
    // React to route param changes (create -> edit or vice versa) without full reload
    '$route.params.id'(id, oldId) {
      if (id && id !== oldId) {
        this.frmData.action = 'update'
        this.loadDetail(id)
      } else if (!id) {
        this.frmData.action = 'create'
        this.loadPrefill()
      }
    }
  },
  methods: {
    bootstrap() {
      this.loadProvinces()
      const id = this.$route?.params?.id
      if (id) {
        this.frmData.action = 'update'
        this.loadDetail(id)
      } else {
        this.loadPrefill()
      }
    },
    formatDate(d) {
      if (!d) return ''
      try {
        const dt = typeof d === 'string' ? new Date(d) : d
        const yyyy = dt.getFullYear()
        const mm = String(dt.getMonth() + 1).padStart(2, '0')
        const dd = String(dt.getDate()).padStart(2, '0')
        return `${dd}/${mm}/${yyyy}`
      } catch (e) {
        return String(d)
      }
    },
    genderText(v) {
      if (v === null || v === undefined) return ''
      const map = { 'M': 'Nam', 'F': 'Nữ', 'O': 'Khác', 1: 'Nam', 0: 'Nữ' }
      return map[v] || (String(v).toLowerCase() === 'male' ? 'Nam' : String(v).toLowerCase() === 'female' ? 'Nữ' : 'Khác')
    },
    async loadProvinces() {
      try {
        const { data } = await axios.get('/provinces')
        this.app.provinces = Array.isArray(data) ? data : []
      } catch (e) {
        this.app.provinces = []
      }
    },
    async loadPrefill() {
      try {
        const { data } = await axios.get('/register-invoices/prefill')
        // Flexible mapping for company and legal representative
        const comp = data.company || {}
        const legal = data.legalRepresentative || data.legalRep || {}
        this.company.address = comp.address || data.contactAddress || ''
        this.company.email = comp.email || data.contactEmail || ''
        this.company.name = comp.name || data.companyName || ''
        this.company.tax_code = comp.taxCode || data.taxCode || ''
        this.legalRep.fullname = legal.fullname || data.legalFullname || ''
        this.legalRep.phone = legal.phone || data.legalPhone || ''
        this.legalRep.citizen_id = legal.citizen_id || legal.citizenId || data.legalCitizenId || ''
        this.legalRep.passport_no = legal.passport_no || legal.passportNo || data.legalPassportNo || ''
        this.legalRep.date_of_birth = legal.date_of_birth || legal.dateOfBirth || data.legalDateOfBirth || null
        this.legalRep.gender = legal.gender != null ? legal.gender : (data.legalGender != null ? data.legalGender : null)
        // Prefill authority name if available
        this.frmData.tax_authority_name = data.taxAuthorityName || ''
        if (!this.frmData.create_date) this.frmData.create_date = this.frmData.declaration_date
      } catch (e) {}
    },
    async loadDetail(id) {
      this.btnLoading = true
      try {
        const { data } = await axios.get(`/register-invoices/${id}`)
        this.applyDetail(data)
        // also refresh prefill to show company/legal info from current user company
        await this.loadPrefill()
      } catch (e) {
        // handle
      } finally {
        this.btnLoading = false
      }
    },
    // --- Normalizers to align backend -> UI values ---
    normalizeArray(raw, mapDict) {
      let arr = []
      if (Array.isArray(raw)) arr = raw
      else if (typeof raw === 'string' && raw.trim()) {
        try {
          let p = JSON.parse(raw)
          // Xử lý JSON bị encode hai lần: nếu kết quả là chuỗi thì parse thêm lần nữa
          if (typeof p === 'string' && p.trim()) {
            try { p = JSON.parse(p) } catch {}
          }
          arr = Array.isArray(p) ? p : []
        } catch { arr = [] }
      } else if (raw && typeof raw === 'object') {
        arr = [raw]
      }
      // Ánh xạ sang mã giao diện nếu có dict
      if (mapDict && Array.isArray(arr)) {
        return arr.map(item => {
          if (typeof item === 'string') return mapDict[item] || item
          if (item && typeof item === 'object') {
            const code = item.code || item.value || item.id || item.type || item.kind
            return mapDict[code] || code
          }
          return item
        }).filter(Boolean)
      }
      return arr
    },
    normalizeInvoiceForms(raw) {
      // Phía backend có thể dùng các mã CMa, KCMa, CMTMTTien; đồng bộ về cùng kiểu
      const dict = { CMa: 'CMa', KCMa: 'KCMa', CMTMTTien: 'CMTMTTien', 'CO_MA': 'CMa', 'KHONG_MA': 'KCMa', 'MA_MAY_TINH_TIEN': 'CMTMTTien' }
      return Array.from(new Set(this.normalizeArray(raw, dict)))
    },
    normalizeInvoiceTypes(raw) {
      const dict = {
        HDGTGT: 'HDGTGT', HDGTGT_BienLai: 'HDGTGT_BienLai', HDBHang: 'HDBHang', HDBHang_BienLai: 'HDBHang_BienLai',
        HDThuongMai: 'HDThuongMai', HDBTSCong: 'HDBTSCong', HDBHDTQGia: 'HDBHDTQGia', HDKhac: 'HDKhac', CTu: 'CTu'
      }
      return Array.from(new Set(this.normalizeArray(raw, dict)))
    },
    normalizeTransferMethods(raw) {
      const dict = { CDDu: 'CDDu', CBTHop: 'CBTHop', 'CHUYEN_DAY_DU': 'CDDu', 'BANG_TONG_HOP': 'CBTHop' }
      return Array.from(new Set(this.normalizeArray(raw, dict)))
    },
    normalizeDigitalCertificates(raw) {
      console.log('normalizeDigitalCertificates input:', raw)
      let arr = []
      
      if (Array.isArray(raw)) {
        // Xử lý mảng có thể chứa chuỗi hoặc object
        for (const item of raw) {
          if (typeof item === 'string' && item.trim()) {
            // Parse malformed string like "{orgName=VNPT, serialNo=123, ...}"
            try {
              // Thử parse JSON trước
              const parsed = JSON.parse(item)
              arr.push(parsed)
            } catch {
              // Xử lý định dạng sai {key=value}
              arr.push(this.parseKeyValueString(item))
            }
          } else if (item && typeof item === 'object') {
            arr.push(item)
          }
        }
      } else if (typeof raw === 'string' && raw.trim()) {
        try {
          let p = JSON.parse(raw)
          if (typeof p === 'string' && p.trim()) {
            try { p = JSON.parse(p) } catch {}
          }
          if (Array.isArray(p)) {
            arr = p
          } else {
            arr = [p]
          }
        } catch {
          // Xử lý dữ liệu sai định dạng như "[{orgName=VNPT, serialNo=123, ...}]"
          arr = [this.parseKeyValueString(raw)]
        }
      } else if (raw && typeof raw === 'object') {
        arr = [raw]
      }
      
      const result = arr.map(x => {
        if (!x || typeof x !== 'object') return null
        
        // Xử lý cả camelCase và snake_case, kèm các key sai định dạng
        const orgName = x.orgName || x.org_name || x.organizationName || x.organization_name || ''
        const serialNo = x.serialNo || x.serial_no || x.serial || x.serialNumber || ''
        const signFromDate = x.signFromDate || x.sign_from_date || x.from || x.fromDate || null
        const signToDate = x.signToDate || x.sign_to_date || x.to || x.toDate || null
        
        // Xử lý sigRegMethod với nhiều định dạng
        let sigRegMethod = x.sigRegMethod || x.sig_reg_method || x.method || x.regMethod || 1
        if (typeof sigRegMethod === 'string') {
          const methodStr = sigRegMethod.toUpperCase()
          if (methodStr === 'EXTEND' || methodStr === 'GIA_HAN') sigRegMethod = 2
          else if (methodStr === 'STOP' || methodStr === 'NGUNG_SU_DUNG') sigRegMethod = 3
          else sigRegMethod = parseInt(sigRegMethod) || 1
        } else if (typeof sigRegMethod !== 'number') {
          sigRegMethod = 1
        }
        
        return {
          orgName,
          serialNo,
          signFromDate,
          signToDate,
          sigRegMethod
        }
      }).filter(x => x !== null)
      
      console.log('normalizeDigitalCertificates result:', result)
      return result
    },
    parseKeyValueString(str) {
      console.log('parseKeyValueString input:', str)
      const obj = {}
      try {
        // Bỏ cặp ngoặc nhọn ngoài cùng nếu có
        let cleanStr = str.trim().replace(/^{|}$/g, '')
        
        // Tách theo dấu phẩy nhưng cẩn thận với dấu phẩy trong giá trị
        const pairs = []
        let current = ''
        let depth = 0
        
        for (let i = 0; i < cleanStr.length; i++) {
          const char = cleanStr[i]
          if (char === '{') depth++
          else if (char === '}') depth--
          else if (char === ',' && depth === 0) {
            pairs.push(current.trim())
            current = ''
            continue
          }
          current += char
        }
        if (current.trim()) pairs.push(current.trim())
        
        pairs.forEach(pair => {
          const equalIndex = pair.indexOf('=')
          if (equalIndex > 0) {
            const key = pair.substring(0, equalIndex).trim()
            const value = pair.substring(equalIndex + 1).trim()
            obj[key] = value
          }
        })
      } catch (e) {
        console.error('Error parsing key-value string:', e)
      }
      
      console.log('parseKeyValueString result:', obj)
      return obj
    },
    applyDetail(data) {
      const val = (obj, keys, def) => {
        for (const k of keys) {
          if (obj && typeof obj[k] !== 'undefined' && obj[k] !== null) return obj[k]
        }
        return def
      }
      const sendMethodsRaw = val(data, ['send_methods', 'sendMethods'], null)
      const sendParsed = (() => {
        if (!sendMethodsRaw) return { a: [], b: [], c: [] }
        if (typeof sendMethodsRaw === 'string') {
          try { return JSON.parse(sendMethodsRaw) } catch { return { a: [], b: [], c: [] } }
        }
        if (typeof sendMethodsRaw === 'object') return sendMethodsRaw
        return { a: [], b: [], c: [] }
      })()
      const createPlaceRaw = val(data, ['create_place','createPlace'], '')
      const createPlaceNum = createPlaceRaw === '' || createPlaceRaw == null ? '' : Number(createPlaceRaw)
      const sigInfo = val(data, ['signature','signatureInfo','signature_info'], null)
      const signatureObj = (sigInfo && typeof sigInfo === 'object' && sigInfo.name) ? sigInfo : (sigInfo ? { name: String(sigInfo) } : null)
      const dateSign = val(data, ['date_sign','dateSign','signatureDate','signDate','sign_date'], null)
      this.frmData = {
        ...this.frmData,
        action: 'update',
        declaration_type: Number(val(data, ['declaration_type','declarationType'], 1)) || 1,
        form_pattern: val(data, ['form_pattern','formPattern'], '01/ĐKTĐ-HĐĐT'),
        declaration_date: val(data, ['declaration_date','declarationDate'], this.frmData.declaration_date),
        tax_authority_name: val(data, ['tax_authority_name','taxAuthorityName'], ''),
        tax_authority_code: val(data, ['tax_authority_code','taxAuthorityCode'], ''),
        invoice_forms: this.normalizeInvoiceForms(val(data, ['invoice_forms','invoiceForms'], [])),
        send_methods_a: this.normalizeArray(val(sendParsed, ['a'], [])),
        send_methods_b: this.normalizeArray(val(sendParsed, ['b'], [])),
        transfer_methods: this.normalizeTransferMethods(val(data, ['transfer_methods','transferMethods'], [])),
        invoice_types: this.normalizeInvoiceTypes(val(data, ['invoice_types','invoiceTypes'], [])),
        digital_certificates: this.normalizeDigitalCertificates(val(data, ['digital_certificates','digitalCertificates'], [])),
        create_place: createPlaceNum,
        signature: signatureObj,
        signed_xml: val(data, ['signed_xml','signedXml'], null),
        date_sign: dateSign,
        status: Number(val(data, ['status'], this.frmData.status)) || 0
      }
      this.sendToggleC = Array.isArray(sendParsed.c) && sendParsed.c.length > 0
    },
    async onSubmit() {
      this.btnLoading = true
      try {
        const payload = this.buildPayload()
        if (this.frmData.action === 'update') {
          await axios.put(`/register-invoices/${this.$route.params.id}/get`, payload, { successMessage: 'Đã cập nhật tờ khai thành công' })
        } else {
          const { data } = await axios.post('/register-invoices', payload, { successMessage: 'Đã tạo tờ khai thành công' })
          this.$router.push({ name: 'CustomerRegisterInvoiceEdit', params: { id: data.id } })
        }
      } catch (e) {
      } finally {
        this.btnLoading = false
      }
    },
    buildPayload() {
      const digitalCertificates = (this.frmData.digital_certificates || []).map(x => ({
        orgName: x.orgName,
        serialNo: x.serialNo,
        signFromDate: x.signFromDate,
        signToDate: x.signToDate,
        sigRegMethod: (typeof x.sigRegMethod === 'number' ? x.sigRegMethod : parseInt(x.sigRegMethod) || 1)
      }))
      return {
        declarationType: this.frmData.declaration_type,
        formPattern: this.frmData.form_pattern,
        declarationDate: this.frmData.declaration_date,
        createPlace: this.frmData.create_place,
        invoiceForms: this.frmData.invoice_forms,
        invoiceTypes: this.frmData.invoice_types,
        sendMethods: { a: this.frmData.send_methods_a, b: this.frmData.send_methods_b, c: this.sendToggleC ? ['CQT'] : [] },
        transferMethods: this.frmData.transfer_methods,
        digitalCertificates,
        solutionProviders: null,
        transmitProviders: null
      }
    },
    async sendData() {
      if (!this.$route?.params?.id) return
      const id = this.$route.params.id
      try {
        let ok = true
        if (typeof window.$?.confirm === 'function') {
          ok = await new Promise(resolve => {
            window.$.confirm({
              title: 'Xác nhận gửi tờ khai',
              content: 'Xác nhận gửi tờ khai lên Cơ quan thuế',
              theme: 'bootstrap',
              type: 'blue',
              icon: 'fas fa-paper-plane',
              animation: 'zoom',
              closeAnimation: 'scale',
              boxWidth: '420px',
              useBootstrap: true,
              backgroundDismiss: true,
              escapeKey: 'cancel',
              buttons: {
                cancel: { text: 'Hủy', btnClass: 'btn-light', action: function(){ resolve(false) } },
                ok: { text: 'Gửi', btnClass: 'btn-primary', action: function(){ resolve(true) } }
              }
            })
          })
        } else {
          ok = window.confirm('Xác nhận gửi tờ khai lên Cơ quan thuế?')
        }
        if (!ok) return
        this.btnLoading = true
        await axios.post(`/register-invoices/${id}/send`, null, { successMessage: 'Đã gửi tờ khai lên Cơ quan thuế' })
        // Refresh detail and start polling for async tax responses
        await this.loadDetail(id)
        this.pollTaxStatus(id)
      } catch (e) {
        // Toast lỗi đã được axios plugin xử lý toàn cục
      } finally {
        this.btnLoading = false
      }
    },
    pollTaxStatus(id) {
      let tries = 0
      let notifiedReceive = false
      let notifiedAccept = false
      const timer = setInterval(async () => {
        try {
          tries++
          const { data } = await axios.get(`/register-invoices/${id}`)
          const status = Number(data?.status)
          // Receive stage (102)
          if (!notifiedReceive && (status === 3 || status === 4)) {
            notifiedReceive = true
            if (status === 4) {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế đã tiếp nhận tờ khai', { title: 'Thông báo', variant: 'success', solid: true, autoHideDelay: 4000 })
            } else {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế không tiếp nhận tờ khai', { title: 'Thông báo', variant: 'warning', solid: true, autoHideDelay: 4000 })
            }
          }
          // Accept stage (103)
          if (!notifiedAccept && (status === 5 || status === 6)) {
            notifiedAccept = true
            if (status === 6) {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế đã chấp nhận tờ khai', { title: 'Thông báo', variant: 'success', solid: true, autoHideDelay: 4000 })
            } else {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế không chấp nhận tờ khai', { title: 'Thông báo', variant: 'danger', solid: true, autoHideDelay: 4000 })
            }
          }
          // Cập nhật UI detail
          this.applyDetail(data)
          // Dừng khi cả hai thông báo hoàn tất hoặc hết thời gian chờ
          if ((notifiedReceive && notifiedAccept) || tries >= 10) {
            clearInterval(timer)
          }
        } catch (err) {
          if (tries >= 10) clearInterval(timer)
        }
      }, 2000)
    },
    goBack() {
      // Quay lại route danh sách một cách rõ ràng
      if (this.$router) {
        this.$router.push({ name: 'registers-invoice' })
      } else if (window && typeof window.location !== 'undefined') {
        window.location.href = '/register/invoice/list'
      }
    },
    async onSignature() {
      try {
        const id = this.$route?.params?.id
        if (!id) return
        let ok = true
        if (typeof window.$?.confirm === 'function') {
          const vm = this
          ok = await new Promise(resolve => {
            window.$.confirm({
              title: 'Xác nhận ký số',
              content: `Thông tin chữ ký và ngày ký sẽ được cập nhật lại. Bạn vẫn muốn tiếp tục ký tờ khai #${id}?`,
              // Visual improvements
              theme: 'bootstrap',
              type: 'blue',
              icon: 'fas fa-signature',
              animation: 'zoom',
              closeAnimation: 'scale',
              animateFromElement: false,
              boxWidth: '420px',
              useBootstrap: true,
              // Layout tweaks
              columnClass: 'col-md-6 offset-md-3',
              backgroundDismiss: true,
              escapeKey: 'cancel',
              buttons: {
                cancel: {
                  text: 'Hủy',
                  btnClass: 'btn-light',
                  action: function () { resolve(false) }
                },
                ok: {
                  text: 'Đồng ý',
                  btnClass: 'btn-primary',
                  action: function () { resolve(true) }
                }
              }
            })
          })
        } else {
          ok = window.confirm(`Xác nhận ký số tờ khai #${id}?`)
        }
        if (!ok) return
        this.btnSignature = true
        const { data } = await axios.post(`/register-invoices/${id}/sign`, null, { successMessage: 'Đã ký số tờ khai thành công' })
        this.frmData.signed_xml = data?.signedXml || data?.signed_xml || this.frmData.signed_xml
        this.frmData.date_sign = data?.signDate || data?.sign_date || new Date().toISOString()
        this.frmData.signature = data?.signatureInfo ? { name: data.signatureInfo } : (data?.signature_info ? { name: data.signature_info } : this.frmData.signature)
        // After signing successfully, move status to 1 so UI shows "Gửi CQT" and hides "Cập nhật"
        this.frmData.status = 1
      } catch (e) {
      } finally {
        this.btnSignature = false
      }
    },
    showSignatureModal(index) {
      if (index !== false && index != null) {
        const v = this.frmData.digital_certificates[index]
        this.signatureModal = { index, orgName: v.orgName, serialNo: v.serialNo, signFromDate: v.signFromDate, signToDate: v.signToDate, sigRegMethod: (typeof v.sigRegMethod === 'number' ? v.sigRegMethod : parseInt(v.sigRegMethod) || 1) }
      } else {
        this.signatureModal = { index: false, orgName: '', serialNo: '', signFromDate: null, signToDate: null, sigRegMethod: 1 }
      }
      this.$refs.modalSignature.show()
    },
    closeSignatureModal() {
      this.$refs.modalSignature.hide()
    },
    onSignatureModalHidden() {
      // cleanup if needed
    },
    onSubmitSignature() {
      const payload = { ...this.signatureModal }
      const entry = {
        orgName: payload.orgName,
        serialNo: payload.serialNo,
        signFromDate: payload.signFromDate,
        signToDate: payload.signToDate,
        sigRegMethod: (typeof payload.sigRegMethod === 'number' ? payload.sigRegMethod : parseInt(payload.sigRegMethod) || 1)
      }
      if (payload.index !== false && payload.index != null) {
        this.$set(this.frmData.digital_certificates, payload.index, entry)
      } else {
        this.frmData.digital_certificates.push(entry)
      }
      this.closeSignatureModal()
    },
    onDeleteSignature(index) {
      this.frmData.digital_certificates.splice(index, 1)
    },
    getNameRegMethod(code) {
      const n = (typeof code === 'number' || /^\d+$/.test(String(code))) ? Number(code) : null
      if (n === 1) return 'Thêm mới'
      if (n === 2) return 'Gia hạn'
      if (n === 3) return 'Ngừng sử dụng'
      return '—'
    },
    mockDetail(id) {
      // no longer used; data comes from backend
      return { id }
    },
  }
}
</script>

<style scoped>
/* Ensure plain text blocks align visually */
.form-control-plaintext {
  padding-left: 0;
  margin-bottom: 0.25rem;
}
.v-select { width: 100%; }
.register-invoice-create .b-form-datepicker { width: 100%; }
</style>
