<template>
  <div class="right_col" role="main">
    <div class="page-content">
      <b-container fluid>
        <div class="page-header mb-2">
          <h1 class="page-title">Hồ sơ</h1>
        </div>

        <!-- Thông tin đơn vị -->
        <b-card class="contact-info">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin đơn vị</h6>
            <div class="edit-icon" v-show="!formInfo">
              <span @click="editInfo"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- View mode -->
          <div class="contact-info-view" v-if="!formInfo">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Mã số thuế:</b-col>
                  <b-col cols="8">{{ frmData.taxCode }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Ngành nghề:</b-col>
                  <b-col cols="8">{{ frmData.companyBusiness }}</b-col>
                </b-row>
              </b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Tên đơn vị:</b-col>
              <b-col cols="10">{{ frmData.companyName }}</b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Địa chỉ:</b-col>
              <b-col cols="10">{{ frmData.companyAddress }}</b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="3" v-if="frmData.companyLogo">
                <p class="font-weight-bold">Logo:</p>
                <img height="50" :src="frmData.companyLogo" />
              </b-col>
              <b-col cols="3" v-if="frmData.companyFavicon">
                <p class="font-weight-bold">Favicon:</p>
                <img height="50" :src="frmData.companyFavicon" />
              </b-col>
            </b-row>
          </div>

          <!-- Edit mode -->
          <b-form @submit="onSubmitInfo" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Mã số thuế"
                  label-for="taxcode"
                  label-class="required"
                >
                  <b-form-input
                    id="taxcode"
                    type="text"
                    placeholder="Mã số thuế doanh nghiệp"
                    disabled
                    v-model="frmData.taxCode"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Ngành nghề"
                  label-for="business"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('companyBusiness')"
                >
                  <b-form-input
                    id="business"
                    type="text"
                    placeholder="Nhập ngành nghề"
                    required
                    v-model="frmInfo.companyBusiness"
                    :state="state('companyBusiness')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12">
                <b-form-group
                  label="Tên đơn vị"
                  label-for="CompanyName"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('companyName')"
                >
                  <b-form-input
                    id="CompanyName"
                    type="text"
                    placeholder="Nhập tên đơn vị"
                    required
                    v-model="frmInfo.companyName"
                    :state="state('companyName')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12">
                <b-form-group
                  label="Địa chỉ"
                  label-for="CompanyAddress"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('companyAddress')"
                >
                  <b-form-input
                    id="CompanyAddress"
                    type="text"
                    placeholder="Nhập địa chỉ đơn vị"
                    required
                    v-model="frmInfo.companyAddress"
                    :state="state('companyAddress')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="3" md="3">
                <b-form-group
                  label="Logo"
                  label-for="logo"
                  class="card p-2 text-center border rounded"
                  :invalid-feedback="invalidFeedback('logo')"
                >
                  <input
                    type="file"
                    ref="logo"
                    class="d-none"
                    @change="onUploadLogo"
                    accept=".png, .jpg, .jpeg"
                  />
                  <b-button
                    variant="success"
                    @click="$refs.logo.click()"
                    class="mb-1"
                    size="sm"
                  >
                    {{ logoName || "Chọn logo" }}
                  </b-button>
                  <p class="font-italic text-success">
                    Kích thước hình tiêu chuẩn 150px x 50px
                  </p>
                </b-form-group>
              </b-col>

              <b-col cols="3" md="3">
                <b-form-group
                  label="Favicon"
                  label-for="Favicon"
                  class="card p-2 text-center border rounded"
                  :invalid-feedback="invalidFeedback('favicon')"
                >
                  <input
                    type="file"
                    ref="favicon"
                    class="d-none"
                    @change="onUploadFavicon"
                    accept=".png, .jpg, .jpeg"
                  />
                  <b-button
                    variant="success"
                    @click="$refs.favicon.click()"
                    class="mb-1"
                    size="sm"
                  >
                    {{ faviconName || "Chọn favicon" }}
                  </b-button>
                  <p class="font-italic text-success">
                    Kích thước hình tiêu chuẩn 50px x 50px
                  </p>
                </b-form-group>
              </b-col>
            </b-row>

            <div class="text-right">
              <b-button size="sm" variant="light" @click="closeForm('formInfo')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" class="btn btn-default" v-if="!buttonInfo">
                Cập nhật
              </b-button>
              <b-button type="button" class="btn btn-default" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Loading...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin người đại diện pháp luật -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin người đại diện pháp luật</h6>
            <div class="edit-icon" v-show="!formRepresent">
              <span @click="editRepresent"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- View mode -->
          <div class="contact-info-view" v-if="!formRepresent">
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Họ và tên:</b-col>
              <b-col cols="4">{{ frmData.representName }}</b-col>
              <b-col cols="2" class="font-weight-bold">Điện thoại:</b-col>
              <b-col cols="4">{{ frmData.representPhone }}</b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Căn cước công dân:</b-col>
              <b-col cols="4">{{ frmData.representCitizenIdent }}</b-col>
              <b-col cols="2" class="font-weight-bold">Số hộ chiếu:</b-col>
              <b-col cols="4">{{ frmData.representPassPort }}</b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Ngày sinh:</b-col>
              <b-col cols="4">{{ formatter(frmData.representDateBirth) }}</b-col>
              <b-col cols="2" class="font-weight-bold">Giới tính:</b-col>
              <b-col cols="4">
                <span>
                  {{
                    frmData.representGender == 1
                      ? "Nam"
                      : frmData.representGender == 0
                      ? "Nữ"
                      : ""
                  }}
                </span>
              </b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Email:</b-col>
              <b-col cols="4">{{ frmData.representMail }}</b-col>
            </b-row>
          </div>

          <!-- Edit mode -->
          <b-form @submit="onsubmitRepresent" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Họ và Tên"
                  label-for="represent-person"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('representName')"
                >
                  <b-form-input
                    id="represent-person"
                    type="text"
                    placeholder="Nhập họ và tên"
                    v-model="frmRepresent.representName"
                    :state="state('representName')"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Điện thoại"
                  label-for="represent-phone"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('representPhone')"
                >
                  <b-form-input
                    id="represent-phone"
                    type="text"
                    placeholder="Nhập số điện thoại"
                    required
                    v-model="frmRepresent.representPhone"
                    :state="state('representPhone')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Căn cước công dân"
                  label-for="represent-citizen-ident"
                  :invalid-feedback="invalidFeedback('representCitizenIdent')"
                >
                  <b-form-input
                    id="represent-citizen-ident"
                    type="text"
                    placeholder="Nhập căn cước công dân"
                    v-model="frmRepresent.representCitizenIdent"
                    :state="state('representCitizenIdent')"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Số hộ chiếu"
                  label-for="represent-pass-port"
                  :invalid-feedback="invalidFeedback('representPassPort')"
                >
                  <b-form-input
                    id="represent-pass-port"
                    type="text"
                    placeholder="Nhập số hộ chiếu"
                    v-model="frmRepresent.representPassPort"
                    :state="state('representPassPort')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Ngày sinh:"
                  label-for="represent-date-birth"
                  label-class="required"
                  breakpoint="md"
                >
                  <b-input-group class="mb-3">
                    <b-form-input
                      id="represent-date-birth"
                      v-model="manualDate"
                      type="text"
                      placeholder="dd/mm/yyyy"
                      @input="parseManualDate"
                      autocomplete="off"
                      :state="state('manualDate')"
                    />
                    <b-input-group-append>
                      <b-form-datepicker
                        v-model="frmRepresent.representDateBirth"
                        locale="vi"
                        size="sm"
                        button-only
                        right
                        :date-format-options="{
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit'
                        }"
                        :min="new Date('1900-01-01')"
                        :max="new Date()"
                        :show-decade-nav="true"
                        @input="syncManualDate"
                      />
                    </b-input-group-append>
                  </b-input-group>
                  <b-form-invalid-feedback :state="state('manualDate')">
                    {{ invalidFeedback('manualDate') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Giới tính"
                  label-for="represent-gender"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('representGender')"
                >
                  <b-form-radio-group
                    id="represent-gender"
                    v-model="frmRepresent.representGender"
                    :options="options.gender"
                    name="gender"
                    :state="state('representGender')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12">
                <b-form-group
                  label="Email"
                  label-for="represent-email"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('representMail')"
                >
                  <b-form-input
                    id="represent-email"
                    type="text"
                    placeholder="Nhập địa chỉ email"
                    required
                    v-model="frmRepresent.representMail"
                    :state="state('representMail')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <div class="text-right">
              <b-button size="sm" variant="light" @click="closeForm('formRepresent')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" class="btn btn-default" v-if="!buttonRepresent">
                Cập nhật
              </b-button>
              <b-button type="button" class="btn btn-default" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Loading...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin hiển thị trên hóa đơn điện tử -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin hiển thị trên hóa đơn điện tử</h6>
            <div class="edit-icon" v-show="!formInvoice">
              <span @click="editInfoInvoice"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- View mode -->
          <div class="contact-info-view" v-if="!formInvoice">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Email:</b-col>
                  <b-col cols="8">{{ frmData.invoiceEmail }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Điện thoại:</b-col>
                  <b-col cols="8">{{ frmData.invoicePhone }}</b-col>
                </b-row>
              </b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Fax:</b-col>
                  <b-col cols="8">{{ frmData.invoiceFax }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Website:</b-col>
                  <b-col cols="8">{{ frmData.invoiceWebsite }}</b-col>
                </b-row>
              </b-col>
            </b-row>
          </div>

          <!-- Edit mode -->
          <b-form @submit="onSubmitInfoInvoice" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Email"
                  label-for="invoice-email"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('invoiceEmail')"
                >
                  <b-form-input
                    id="invoice-email"
                    type="email"
                    placeholder="Nhập địa chỉ email"
                    v-model="frmDataInvoice.invoiceEmail"
                    :state="state('invoiceEmail')"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Điện thoại"
                  label-for="invoice-phone"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('invoicePhone')"
                >
                  <b-form-input
                    id="invoice-phone"
                    type="text"
                    placeholder="Nhập số điện thoại"
                    required
                    v-model="frmDataInvoice.invoicePhone"
                    :state="state('invoicePhone')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Fax"
                  label-for="invoice-fax"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('invoiceFax')"
                >
                  <b-form-input
                    id="invoice-fax"
                    type="text"
                    placeholder="Nhập số fax"
                    required
                    v-model="frmDataInvoice.invoiceFax"
                    :state="state('invoiceFax')"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Website"
                  label-for="invoice-website"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('invoiceWebsite')"
                >
                  <b-form-input
                    id="invoice-website"
                    type="text"
                    placeholder="Nhập địa chỉ website"
                    required
                    v-model="frmDataInvoice.invoiceWebsite"
                    :state="state('invoiceWebsite')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <div class="text-right">
              <b-button size="sm" variant="light" @click="closeForm('formInvoice')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" class="btn btn-default" v-if="!buttonInfoInvoice">
                Cập nhật
              </b-button>
              <b-button type="button" class="btn btn-default" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Loading...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin hiển thị trên các hồ sơ gửi cơ quan thuế -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin hiển thị trên các hồ sơ gửi cơ quan thuế</h6>
            <div class="edit-icon" v-show="!formContact">
              <span @click="editInfoContact"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- View mode -->
          <div class="contact-info-view" v-if="!formContact">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Họ và Tên:</b-col>
                  <b-col cols="8">{{ frmData.contactName }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Email:</b-col>
                  <b-col cols="8">{{ frmData.contactMail }}</b-col>
                </b-row>
              </b-col>
            </b-row>

            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Phone:</b-col>
                  <b-col cols="8">{{ frmData.contactPhone }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Địa chỉ:</b-col>
                  <b-col cols="8">{{ frmData.contactAddress }}</b-col>
                </b-row>
              </b-col>
            </b-row>
          </div>

          <!-- Edit mode -->
          <b-form @submit="onSubmitContact" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Họ và Tên"
                  label-for="contact-name"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('contactName')"
                >
                  <b-form-input
                    id="contact-name"
                    type="text"
                    placeholder="Nhập họ và tên"
                    v-model.trim="frmDataContact.contactName"
                    :state="state('contactName')"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Email"
                  label-for="contact-email"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('contactMail')"
                >
                  <b-form-input
                    id="contact-email"
                    type="email"
                    placeholder="Nhập địa chỉ email"
                    required
                    v-model.trim="frmDataContact.contactMail"
                    :state="state('contactMail')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Điện thoại"
                  label-for="contact-phone"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('contactPhone')"
                >
                  <b-form-input
                    id="contact-phone"
                    type="text"
                    placeholder="Nhập số điện thoại"
                    required
                    v-model.trim="frmDataContact.contactPhone"
                    :state="state('contactPhone')"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Địa chỉ"
                  label-for="contact-address"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('contactAddress')"
                >
                  <b-form-input
                    id="contact-address"
                    type="text"
                    placeholder="Nhập địa chỉ"
                    required
                    v-model.trim="frmDataContact.contactAddress"
                    :state="state('contactAddress')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <div class="text-right">
              <b-button size="sm" variant="light" @click="closeForm('formContact')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" class="btn btn-default" v-if="!buttonContact">
                Cập nhật
              </b-button>
              <b-button type="button" class="btn btn-default" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Loading...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin thanh toán -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin thanh toán</h6>
            <div class="edit-icon" v-show="!formBank">
              <span @click="editBank"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- View mode -->
          <div class="contact-info-view" v-if="!formBank">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="12">
                <b-row>
                  <b-col cols="2" class="font-weight-bold">Số tài khoản ngân hàng:</b-col>
                  <b-col cols="10">{{ frmData.bankNo }}</b-col>
                </b-row>
              </b-col>
            </b-row>

            <b-row class="pt-2 pb-2">
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Tên ngân hàng:</b-col>
                  <b-col cols="8">{{ frmData.bankName }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Chi nhánh:</b-col>
                  <b-col cols="8">{{ frmData.bankBrand || frmData.bankAddress }}</b-col>
                </b-row>
              </b-col>
            </b-row>
          </div>

          <!-- Edit mode -->
          <b-form @submit="onSubmitBank" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Số tài khoản ngân hàng"
                  label-for="bank-no"
                  label-class="required"
                  :invalid-feedback="invalidFeedback('bankNo')"
                >
                  <b-form-input
                    id="bank-no"
                    type="text"
                    placeholder="Nhập số tài khoản ngân hàng"
                    v-model="frmDataBank.bankNo"
                    :state="state('bankNo')"
                    required
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Tên ngân hàng"
                  label-for="bank-name"
                  label-class="required"
                >
                  <v-select
                    id="bank-name"
                    v-model="frmDataBank.bankName"
                    :options="options.banks"
                    label="name"
                    placeholder="Chọn ngân hàng"
                    :reduce="(bank) => bank.abbreviation"
                    append-to-body
                    :calculate-position="withPopper"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Chi nhánh"
                  label-for="bank-brand"
                  :invalid-feedback="invalidFeedback('bankBrand')"
                >
                  <b-form-input
                    id="bank-brand"
                    type="text"
                    placeholder="Nhập chi nhánh"
                    v-model="frmDataBank.bankBrand"
                    :state="state('bankBrand')"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <div class="text-right">
              <b-button size="sm" variant="light" @click="closeForm('formBank')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" class="btn btn-default" v-if="!buttonBank">
                Cập nhật
              </b-button>
              <b-button type="button" class="btn btn-default" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Loading...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin đơn vị chủ quản -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin đơn vị chủ quản</h6>
            <div class="edit-icon" v-show="!formTaxAuthority">
              <span @click="editTaxAuthority"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- View mode -->
          <div class="contact-info-view" v-if="!formTaxAuthority">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Cục thuế Tỉnh/Thành:</b-col>
                  <b-col cols="8">{{ frmData.bladeTaxAuthorityCity }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Cơ quan thuế quản lý:</b-col>
                  <b-col cols="8">{{ frmData.bladeTaxAuthorityName }}</b-col>
                </b-row>
              </b-col>
            </b-row>
          </div>

          <!-- Edit mode -->
          <b-form @submit="onSubmitTaxAuthority" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Cục thuế Tỉnh/Thành"
                  label-for="authority-city"
                  label-class="required"
                >
                  <v-select
                    id="authority-city"
                    @input="getTaxAuthority"
                    v-model="frmTaxAuthority.taxAuthorityCity"
                    :options="options.taxAuthorities"
                    label="name"
                    placeholder="Chọn cục thuế Tỉnh/Thành"
                    :reduce="(item) => item.code"
                    append-to-body
                    :calculate-position="withPopper"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Cơ quan thuế quản lý"
                  label-for="authority-name"
                  label-class="required"
                >
                  <v-select
                    id="authority-name"
                    v-model="frmTaxAuthority.taxAuthorityName"
                    :options="options.taxAuthorityNames"
                    label="name"
                    placeholder="Chọn cơ quan thuế quản lý"
                    :reduce="(item) => item.code"
                    append-to-body
                    :calculate-position="withPopper"
                  />
                </b-form-group>
              </b-col>
            </b-row>

            <div class="text-right">
              <b-button size="sm" variant="light" @click="closeForm('formTaxAuthority')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" class="btn btn-default" v-if="!buttonTaxAuthority">
                Cập nhật
              </b-button>
              <b-button type="button" size="sm" class="btn btn-default" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Loading...
              </b-button>
            </div>
          </b-form>
        </b-card>
      </b-container>
    </div>
  </div>
</template>

<script>
import Vue from "vue";
import axios from "@/plugins/axios";
import vSelect from "vue-select";
import "vue-select/dist/vue-select.css";
import moment from "moment";
import { createPopper as Popperjs } from "@popperjs/core";

export default {
  name: "Profile",
  components: { vSelect },
  data() {
    return {
      // Toggle forms
      formInfo: false,
      formRepresent: false,
      formInvoice: false,
      formContact: false,
      formBank: false,
      formTaxAuthority: false,

      // Loading flags
      buttonInfo: false,
      buttonRepresent: false,
      buttonInfoInvoice: false,
      buttonContact: false,
      buttonBank: false,
      buttonTaxAuthority: false,

      // Main data + form models
      frmData: {},
      frmInfo: {},
      frmRepresent: {},
      frmDataInvoice: {},
      frmDataContact: {},
      frmDataBank: {
        bankName: null,
        bankNo: null,
        bankAddress: null,
        bankBrand: null,
        bankNameCustom: null,
        bankSeparatorCharacter: null
      },
      frmTaxAuthority: {
        taxAuthorityCity: null,
        taxAuthorityName: null
      },

      // Options data
      options: {
        banks: [],
        taxAuthorities: [],
        taxAuthorityNames: [],
        gender: [
          { text: "Nam", value: 1 },
          { text: "Nữ", value: 0 }
        ]
      },

      // Validation + helpers
      errors: {},
      logoName: null,
      faviconName: null,
      manualDate: ""
    };
  },

  mounted() {
    const vm = this;

    axios.post("/setting/profile/ini").then((response) => {
      vm.options.banks = Array.isArray(response.data.banks)
        ? response.data.banks
        : [];
      vm.options.taxAuthorities = Array.isArray(response.data.taxAuthorities)
        ? response.data.taxAuthorities
        : [];
    });

    Vue.nextTick(() => {
      vm.importData();
    });
  },

  methods: {
    // File uploads
    onUploadLogo(e) {
      const files = e.target.files || e.dataTransfer.files;
      if (!files.length) return;
      this.frmInfo.logo = files[0];
      this.logoName = files[0].name;
    },

    onUploadFavicon(e) {
      const files = e.target.files || e.dataTransfer.files;
      if (!files.length) return;
      this.frmInfo.favicon = files[0];
      this.faviconName = files[0].name;
    },

    // Toggle edits and preload form data
    editInfo() {
      this.frmInfo.companyName = this.frmData.companyName || "";
      this.frmInfo.companyAddress = this.frmData.companyAddress || "";
      this.frmInfo.companyBusiness = this.frmData.companyBusiness || "";
      this.frmInfo.logo = null;
      this.frmInfo.favicon = null;
      this.formInfo = true;
    },

    editRepresent() {
      this.frmRepresent.representName = this.frmData.representName || "";
      this.frmRepresent.representPhone = this.frmData.representPhone || "";
      this.frmRepresent.representCitizenIdent =
        this.frmData.representCitizenIdent || "";
      this.frmRepresent.representPassPort = this.frmData.representPassPort || "";
      this.frmRepresent.representDateBirth =
        this.frmData.representDateBirth || null;
      this.frmRepresent.representGender = this.frmData.representGender;
      this.frmRepresent.representMail = this.frmData.representMail || "";
      this.formRepresent = true;

      if (this.frmRepresent.representDateBirth) {
        this.syncManualDate(this.frmRepresent.representDateBirth);
      }
    },

    editInfoInvoice() {
      this.frmDataInvoice.invoiceEmail = this.frmData.invoiceEmail || "";
      this.frmDataInvoice.invoicePhone = this.frmData.invoicePhone || "";
      this.frmDataInvoice.invoiceFax = this.frmData.invoiceFax || "";
      this.frmDataInvoice.invoiceWebsite = this.frmData.invoiceWebsite || "";
      this.formInvoice = true;
    },

    editInfoContact() {
      // clear previous errors for contact fields before opening the form
      const newErrors = { ...this.errors }
      delete newErrors.contactName
      delete newErrors.contactMail
      delete newErrors.contactPhone
      delete newErrors.contactAddress
      this.errors = newErrors

      this.frmDataContact.contactName = this.frmData.contactName || ""
      this.frmDataContact.contactMail = this.frmData.contactMail || ""
      this.frmDataContact.contactPhone = this.frmData.contactPhone || ""
      this.frmDataContact.contactAddress = this.frmData.contactAddress || ""
      this.formContact = true
    },

    editBank() {
      this.frmDataBank.bankName = this.frmData.bankAbbreviation || null;
      this.frmDataBank.bankNo = this.frmData.bankNo || null;
      this.frmDataBank.bankAddress = this.frmData.bankAddress || null;
      this.frmDataBank.bankBrand = this.frmData.bankBrand || null;
      this.frmDataBank.bankNameCustom = this.frmData.bankNameCustom || null;
      this.frmDataBank.bankSeparatorCharacter =
        this.frmData.bankSeparatorCharacter || null;
      this.formBank = true;
    },

    editTaxAuthority() {
      this.frmTaxAuthority.taxAuthorityCity = this.frmData.taxAuthorityCity || null;
      this.frmTaxAuthority.taxAuthorityName = this.frmData.taxAuthorityName || null;
      this.formTaxAuthority = true;
    },

    // Load tax authority names by city code
    getTaxAuthority(code) {
      const vm = this;
      vm.options.taxAuthorityNames = [];
      vm.frmTaxAuthority.taxAuthorityName = null;

      const parsedCode = parseInt(code, 10) || 0;
      if (parsedCode > 0) {
        axios
          .post("/setting/profile/get-tax-authority", { parentCode: parsedCode })
          .then((response) => {
            if (response.data === 0) {
              vm.frmData.taxAuthorityCity = vm.frmTaxAuthority.taxAuthorityCity = null;
              vm.frmData.taxAuthorityName = vm.frmTaxAuthority.taxAuthorityName = null;
              return;
            }
            if (!response.data || Object.keys(response.data).length < 1) {
              vm.frmData.taxAuthorityName = vm.frmTaxAuthority.taxAuthorityName = null;
              return;
            }
            vm.options.taxAuthorityNames = response.data;
          });
      }
      return false;
    },

    // Close forms
    closeForm(value) {
      switch (value) {
        case "formInfo":
          this.formInfo = false;
          this.frmInfo = {};
          break;
        case "formRepresent":
          this.formRepresent = false;
          this.frmRepresent = {};
          break;
        case "formInvoice":
          this.formInvoice = false;
          this.frmDataInvoice = {};
          break;
        case "formContact":
          this.formContact = false
          this.frmDataContact = {}
          // clear errors of contact form when closing
          const newErrors2 = { ...this.errors }
          delete newErrors2.contactName
          delete newErrors2.contactMail
          delete newErrors2.contactPhone
          delete newErrors2.contactAddress
          this.errors = newErrors2
          break;
        case "formBank":
          this.formBank = false;
          this.frmDataBank = {
            bankName: null,
            bankNo: null,
            bankAddress: null,
            bankBrand: null,
            bankNameCustom: null,
            bankSeparatorCharacter: null
          };
          break;
        case "formTaxAuthority":
          this.formTaxAuthority = false;
          this.frmTaxAuthority = {
            taxAuthorityCity: null,
            taxAuthorityName: null
          };
          break;
        default:
      }
      return false;
    },

    // Import main data
    importData() {
      axios
        .post("/setting/profile/get")
        .then((response) => {
          this.frmData = response.data || {};
          if (this.frmData.taxAuthorityCity) {
            this.getTaxAuthority(this.frmData.taxAuthorityCity);
          }
        })
        .catch(() => {
          this.$router.push("/");
        });
    },

    // Submit handlers
    onSubmitInfo(e) {
      e.preventDefault();
      this.buttonInfo = true;

      const frmData = new FormData();
      frmData.append("companyName", this.frmInfo.companyName || "");
      frmData.append("companyAddress", this.frmInfo.companyAddress || "");
      frmData.append("companyBusiness", this.frmInfo.companyBusiness || "");
      if (this.frmInfo.logo) frmData.append("logo", this.frmInfo.logo);
      if (this.frmInfo.favicon) frmData.append("favicon", this.frmInfo.favicon);

      axios
        .post("/setting/profile/update-info", frmData, {
          headers: { "Content-Type": "multipart/form-data" }
        })
        .then(async () => {
          this.buttonInfo = false;
          this.formInfo = false;
          this.frmInfo = {};
          this.importData();
          
          // Refresh user info from server to get updated company data
          try {
            const infoRes = await axios.get('/auth/info', { meta: { suppressGlobalErrorToast: true } });
            if (infoRes.data) {
              // Update global app state with fresh data
              this.$app.info = infoRes.data;
            }
          } catch (error) {
            console.warn('Failed to refresh auth info:', error);
            // Fallback to loadAppInfo if /auth/info fails
            this.$app.loadAppInfo();
          }
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonInfo = false;
        });
    },

    onsubmitRepresent(e) {
      e.preventDefault();
      this.buttonRepresent = true;

      axios
        .post("/setting/profile/update-represent", this.frmRepresent)
        .then(() => {
          this.buttonRepresent = false;
          this.formRepresent = false;
          this.frmRepresent = {};
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonRepresent = false;
        });
    },

    onSubmitInfoInvoice(e) {
      e.preventDefault();
      this.buttonInfoInvoice = true;

      axios
        .post("/setting/profile/update-info-invoice", this.frmDataInvoice)
        .then(() => {
          this.buttonInfoInvoice = false;
          this.formInvoice = false;
          this.frmDataInvoice = {};
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonInfoInvoice = false;
        });
    },

    onSubmitContact(e) {
      e.preventDefault();
      this.buttonContact = true;

      axios
        .post("/setting/profile/update-contact", this.frmDataContact)
        .then(() => {
          this.buttonContact = false;
          this.formContact = false;
          this.frmDataContact = {};
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonContact = false;
        });
    },

    onSubmitBank(e) {
      e.preventDefault();
      this.buttonBank = true;

      axios
        .post("/setting/profile/update-bank", this.frmDataBank)
        .then(() => {
          this.buttonBank = false;
          this.closeForm("formBank");
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonBank = false;
        });
    },

    onSubmitTaxAuthority(e) {
      e.preventDefault();
      this.buttonTaxAuthority = true;

      axios
        .post("/setting/profile/update-tax-authority", this.frmTaxAuthority)
        .then(() => {
          this.buttonTaxAuthority = false;
          this.closeForm("formTaxAuthority");
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonTaxAuthority = false;
        });
    },

    // Validation helpers
    state(field) {
      const errors = this.errors || {};
      if (!Object.prototype.hasOwnProperty.call(errors, field)) {
        return null; // neutral
      }
      return false; // invalid
    },

    invalidFeedback(field) {
      const errors = this.errors || {};
      if (!Object.prototype.hasOwnProperty.call(errors, field)) {
        return "";
      }
      return (errors[field] || []).join("");
    },

    // v-select popper positioning
    withPopper(dropdownList, component, { width }) {
      dropdownList.style.width = width;
      const popper = Popperjs(component.$refs.toggle, dropdownList, {
        placement: "bottom",
        modifiers: [
          {
            name: "offset",
            options: {
              offset: [0, -1]
            }
          },
          {
            name: "toggleClass",
            enabled: true,
            phase: "write",
            fn({ state }) {
              component.$el.classList.toggle("drop-up", state.placement === "top");
            }
          }
        ]
      });
      return () => popper.destroy();
    },

    // Date handling for manual input + datepicker sync
    parseManualDate() {
      const regex =
        /^(0[1-9]|[12][0-9]|3[01])\/(0[1-9]|1[0-2])\/(19\d{2}|20\d{2})$/;

      // reset field errors first
      const newErrors = { ...this.errors };
      delete newErrors.manualDate;
      this.errors = newErrors;

      if (!regex.test(this.manualDate)) {
        this.errors = {
          ...this.errors,
          manualDate: ["Ngày không hợp lệ. Vui lòng nhập đúng định dạng dd/mm/yyyy"]
        };
        return;
      }

      const [day, month, year] = this.manualDate.split("/");
      const iso = `${year}-${month}-${day}`;
      const parsed = new Date(iso);

      if (isNaN(parsed.getTime())) {
        this.errors = {
          ...this.errors,
          manualDate: ["Ngày không hợp lệ."]
        };
        return;
      }

      this.frmRepresent.representDateBirth = parsed;

      if (this.frmRepresent.representDateBirth) {
        this.syncManualDate(this.frmRepresent.representDateBirth);
      }
    },

    syncManualDate(date) {
      const parsed = new Date(date);
      if (!isNaN(parsed)) {
        const day = String(parsed.getDate()).padStart(2, "0");
        const month = String(parsed.getMonth() + 1).padStart(2, "0");
        const year = parsed.getFullYear();
        this.manualDate = `${day}/${month}/${year}`;

        // clear errors for manualDate
        const newErrors = { ...this.errors };
        delete newErrors.manualDate;
        this.errors = newErrors;
      } else {
        this.manualDate = "";
        this.errors = {
          ...this.errors,
          manualDate: ["Ngày không hợp lệ."]
        };
      }
    },

    formatter(value) {
      if (value) {
        return moment(value).format("DD/MM/YYYY");
      }
      return "";
    }
  }
};
</script>

<style scoped>
.contact-info {
  border: 1px solid #eaeaea;
  border-radius: 6px;
  padding: 12px;
}

.title-contact {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.edit-icon {
  cursor: pointer;
}

.v-select {
  width: 100%;
}
</style>