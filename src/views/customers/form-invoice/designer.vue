<template>
  <div
    class="container-fluid py-3 template-designer"
    :class="{ 'designer-expanded': expanded }"
  >
    <div class="designer-heading">
      <div>
        <h3 class="mb-0">Thiết kế mẫu hóa đơn</h3>
      </div>
      <div class="d-flex action-bar">
        <b-button
          variant="outline-secondary"
          :aria-pressed="expanded"
          @click="expanded = !expanded"
        >
          {{ expanded ? "Thu gọn" : "Mở rộng" }}
        </b-button>
        <b-button variant="secondary" @click="$router.back()">Trở lại</b-button>
      </div>
    </div>
    <b-alert
      v-if="error"
      show
      dismissible
      variant="danger"
      @dismissed="error = ''"
      >{{ error }}</b-alert
    >
    <b-alert
      v-if="notice"
      show
      dismissible
      variant="success"
      @dismissed="notice = ''"
      >{{ notice }}</b-alert
    >
    <template v-if="details && draft">
      <div class="document-header">
        <div class="d-flex align-items-center action-bar">
          <strong>{{ details.template.name }}</strong>
          <b-badge :variant="saveStateVariant">{{ saveStateLabel }}</b-badge>
          <span v-if="draft.baseVersionId" class="text-muted small">
            {{ draftSourceLabel }}
          </span>
        </div>
        <div class="d-flex flex-wrap action-bar">
          <b-button
            size="sm"
            variant="outline-primary"
            :disabled="busy"
            @click="preview"
            >Bản in PDF</b-button
          >
          <b-button
            size="sm"
            variant="primary"
            :disabled="busy"
            @click="saveDraftOnly"
          >
            Lưu nháp
          </b-button>
          <b-button
            size="sm"
            variant="success"
            :disabled="busy"
            @click="saveVersion"
          >
            {{ busy ? "Đang xử lý…" : "Lưu phiên bản" }}
          </b-button>
        </div>
      </div>
      <div class="designer-ribbon">
        <div class="ribbon-tabs" role="tablist" aria-label="Công cụ thiết kế">
          <button
            v-for="tab in editorTabs"
            :key="tab.value"
            type="button"
            role="tab"
            :aria-selected="activeTab === tab.value"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.text }}
          </button>
        </div>
        <fieldset v-if="layout" :disabled="busy" class="ribbon-content">
          <div v-if="activeTab === 'home'" class="format-toolbar">
            <div class="toolbar-group">
              <button
                type="button"
                title="Hoàn tác (Ctrl+Z)"
                :disabled="!canUndo"
                @click="undo"
              >
                ↶
              </button>
              <button
                type="button"
                title="Làm lại (Ctrl+Y)"
                :disabled="!canRedo"
                @click="redo"
              >
                ↷
              </button>
            </div>
            <div class="toolbar-group">
              <select
                v-model="layout.theme.fontAssetFamily"
                aria-label="Phông chữ"
              >
                <option value="body">Times New Roman</option>
                <option value="sans">Arial</option>
              </select>
              <input
                :value="selectedStyle('fontSizePt', layout.theme.fontSizePt)"
                type="number"
                min="7"
                max="36"
                class="font-size-input"
                aria-label="Cỡ chữ"
                @change="setStyle('fontSizePt', Number($event.target.value))"
              />
            </div>
            <div class="toolbar-group">
              <button
                type="button"
                title="In đậm"
                :class="{ active: selectedStyle('fontWeight', 400) === 700 }"
                @click="
                  setStyle(
                    'fontWeight',
                    selectedStyle('fontWeight', 400) === 700 ? 400 : 700
                  )
                "
              >
                <strong>B</strong>
              </button>
              <button
                type="button"
                title="In nghiêng"
                :class="{
                  active: selectedStyle('fontStyle', 'normal') === 'italic',
                }"
                @click="
                  setStyle(
                    'fontStyle',
                    selectedStyle('fontStyle', 'normal') === 'italic'
                      ? 'normal'
                      : 'italic'
                  )
                "
              >
                <em>I</em>
              </button>
              <label class="color-tool" title="Màu chữ">
                <span
                  class="color-tool-symbol"
                  :style="{
                    borderBottomColor: selectedStyle(
                      'color',
                      layout.theme.color
                    ),
                  }"
                  aria-hidden="true"
                  >A</span
                >
                <input
                  type="color"
                  :value="selectedStyle('color', layout.theme.color)"
                  aria-label="Màu chữ"
                  @input="setStyle('color', $event.target.value)"
              /></label>
              <label class="color-tool" title="Màu nền">
                <span
                  class="color-tool-symbol color-tool-fill"
                  :style="{
                    backgroundColor: selectedStyle(
                      'backgroundColor',
                      '#ffffff'
                    ),
                  }"
                  aria-hidden="true"
                  >A</span
                >
                <input
                  type="color"
                  :value="selectedStyle('backgroundColor', '#ffffff')"
                  aria-label="Màu nền"
                  @input="setStyle('backgroundColor', $event.target.value)"
              /></label>
            </div>
            <div class="toolbar-group">
              <button
                v-for="align in alignments"
                :key="align.value"
                type="button"
                :title="align.text"
                :class="{
                  active: selectedStyle('textAlign', 'left') === align.value,
                }"
                @click="setStyle('textAlign', align.value)"
              >
                <span
                  :class="['alignment-icon', align.value]"
                  aria-hidden="true"
                  ><i /><i /><i
                /></span>
              </button>
            </div>
            <div class="toolbar-group">
              <button
                type="button"
                :disabled="!selected || selected.id === layout.body.id"
                @click="duplicateSelected"
              >
                Nhân đôi
              </button>
              <button
                type="button"
                :disabled="!selected || selected.id === layout.body.id"
                @click="deleteSelected"
              >
                Xóa
              </button>
            </div>
          </div>
          <div v-else-if="activeTab === 'insert'" class="insert-toolbar">
            <button
              v-for="type in blockTypes"
              :key="type.value"
              type="button"
              draggable="true"
              @dragstart="dragPalette($event, type.value)"
              @dragend="finishPaletteDrag"
              @click="addBlock(type.value)"
            >
              {{ type.text }}
            </button>
            <label class="btn btn-sm btn-outline-secondary mb-0"
              >Ảnh / logo
              <input
                type="file"
                class="d-none"
                accept="image/png,image/jpeg"
                @change="uploadImage"
              />
            </label>
          </div>
          <div v-else-if="activeTab === 'page'" class="page-toolbar">
            <label class="page-setting"
              >Khổ giấy
              <b-form-select
                size="sm"
                v-model="layout.page.format"
                :options="['A4', 'A5']"
            /></label>
            <label class="page-setting page-orientation-setting"
              >Hướng trang
              <b-form-select
                size="sm"
                v-model="layout.page.orientation"
                :options="[
                  { value: 'portrait', text: 'Dọc' },
                  { value: 'landscape', text: 'Ngang' },
                ]"
            /></label>
            <label
              v-for="(value, side) in layout.page.marginMm"
              :key="side"
              class="page-setting page-margin-setting"
              >{{ marginLabels[side] }} (mm)
              <b-form-input
                size="sm"
                v-model.number="layout.page.marginMm[side]"
                type="number"
                min="3"
                max="40"
              />
            </label>
            <label class="page-setting page-font-setting"
              >Cỡ chữ mặc định
              <b-form-input
                size="sm"
                v-model.number="layout.theme.fontSizePt"
                type="number"
                min="7"
                max="32"
            /></label>
            <label class="page-setting page-line-setting"
              >Giãn dòng
              <b-form-input
                size="sm"
                v-model.number="layout.theme.lineHeight"
                type="number"
                min="1"
                max="2"
                step="0.1"
            /></label>
          </div>
        </fieldset>
        <div v-if="activeTab === 'versions'" class="versions-panel">
          <div class="versions-header">
            <div>
              <strong>Lịch sử lưu mẫu</strong>
              <span>{{
                details.versions.length
                  ? `${details.versions.length} bản đã lưu`
                  : "Chưa có bản đã lưu"
              }}</span>
            </div>
          </div>
          <div v-if="details.versions.length" class="version-list">
            <div
              v-for="(version, index) in details.versions"
              :key="version.id"
              class="version-row"
            >
              <div class="version-info">
                <strong>Mẫu đã lưu</strong>
                <span>{{ formatVersionDate(version.publishedAt) }}</span>
                <span v-if="index === 0" class="version-latest">Mới nhất</span>
              </div>
              <div>
                <b-button
                  size="sm"
                  variant="outline-primary"
                  :disabled="busy"
                  @click="restore(version)"
                  >Chỉnh sửa từ bản này</b-button
                >
                <b-button
                  size="sm"
                  variant="outline-danger"
                  class="ml-2"
                  :disabled="busy || index === 0"
                  :title="index === 0 ? 'Bản đang áp dụng' : 'Xóa bản này'"
                  @click="deleteVersion(version)"
                  >Xóa</b-button
                >
              </div>
            </div>
          </div>
          <div v-else class="versions-empty">Chưa có bản đã lưu</div>
        </div>
      </div>
      <div v-if="layout" class="workspace-toolbar">
        <div class="workspace-panels">
          <button
            type="button"
            :class="{ active: showSidebar }"
            :aria-pressed="showSidebar"
            @click="togglePanel('showSidebar')"
          >
            Phần tử
          </button>
          <button
            type="button"
            :class="{ active: showInspector }"
            :aria-pressed="showInspector"
            @click="togglePanel('showInspector')"
          >
            Thuộc tính
          </button>
        </div>
        <label class="sample-rows" for="invoice-sample-rows"
          >Dòng minh họa
          <b-form-select
            id="invoice-sample-rows"
            size="sm"
            v-model.number="sampleRows"
            :options="[0, 1, 3, 10, 60]"
          />
        </label>
        <div class="zoom-control">
          <button type="button" @click="fitPage">Vừa chiều ngang</button>
          <button
            type="button"
            aria-label="Thu nhỏ"
            :disabled="zoom <= 40"
            @click="zoom = Math.max(40, zoom - 5)"
          >
            −
          </button>
          <input
            id="invoice-designer-zoom"
            v-model.number="zoom"
            type="range"
            min="40"
            max="150"
            step="5"
            aria-label="Thu phóng"
          />
          <button
            type="button"
            aria-label="Phóng to"
            :disabled="zoom >= 150"
            @click="zoom = Math.min(150, zoom + 5)"
          >
            +
          </button>
          <span>{{ zoom }}%</span>
        </div>
      </div>
      <div
        v-if="layout"
        class="designer-workspace"
        :class="{
          'sidebar-hidden': !showSidebar,
          'inspector-hidden': !showInspector,
        }"
      >
        <aside
          v-show="showSidebar"
          class="designer-sidebar"
          aria-label="Phần tử và cấu trúc"
        >
          <div class="panel-tabs" role="tablist" aria-label="Danh sách phần tử">
            <button
              type="button"
              role="tab"
              :aria-selected="sidebarTab === 'elements'"
              :class="{ active: sidebarTab === 'elements' }"
              @click="sidebarTab = 'elements'"
            >
              Thêm phần tử
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="sidebarTab === 'xml'"
              aria-label="Phần tử XML"
              :class="{ active: sidebarTab === 'xml' }"
              @click="sidebarTab = 'xml'"
            >
              XML
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="sidebarTab === 'outline'"
              :class="{ active: sidebarTab === 'outline' }"
              @click="sidebarTab = 'outline'"
            >
              Cấu trúc
            </button>
          </div>
          <div v-show="sidebarTab !== 'outline'" class="sidebar-content">
            <b-form-input
              v-model.trim="elementSearch"
              size="sm"
              type="search"
              :placeholder="
                sidebarTab === 'xml' ? 'Tìm tên hoặc mã XML…' : 'Tìm phần tử…'
              "
              aria-label="Tìm phần tử"
            />
            <div v-if="sidebarTab === 'xml'" class="xml-field-palette">
              <div class="xml-field-heading">
                <div class="palette-group-title">Phần tử XML</div>
                <small>{{ visibleXmlFields.length }} phần tử</small>
              </div>
              <b-form-select
                v-model="xmlGroupFilter"
                size="sm"
                class="xml-group-select"
                :options="xmlGroupOptions"
                aria-label="Nhóm phần tử XML"
              />
              <div class="block-palette xml-field-list">
                <button
                  v-for="field in visibleXmlFields"
                  :key="field.key"
                  type="button"
                  draggable="true"
                  :disabled="busy"
                  :title="`${field.label} (${field.key})`"
                  @dragstart="dragXmlField($event, field)"
                  @dragend="finishPaletteDrag"
                  @click="addField(field)"
                >
                  <span class="palette-icon xml-field-icon" aria-hidden="true"
                    >{ }</span
                  >
                  <span class="xml-field-copy">
                    <span class="xml-field-label">{{ field.label }}</span>
                  </span>
                  <span class="xml-field-add" aria-hidden="true">+</span>
                </button>
              </div>
              <p v-if="!visibleXmlFields.length" class="xml-field-empty">
                Không có phần tử phù hợp
              </p>
            </div>
            <template v-if="sidebarTab === 'elements'">
              <div
                v-for="group in paletteGroups"
                :key="group.label"
                class="palette-section"
              >
                <div class="palette-group-title">{{ group.label }}</div>
                <div class="block-palette">
                  <button
                    v-for="type in group.items"
                    :key="type.value"
                    type="button"
                    draggable="true"
                    :disabled="busy"
                    @dragstart="dragPalette($event, type.value)"
                    @dragend="finishPaletteDrag"
                    @click="addBlock(type.value)"
                  >
                    <span class="palette-icon" aria-hidden="true">{{
                      blockIcons[type.value]
                    }}</span
                    ><span>{{ type.text }}</span>
                  </button>
                </div>
              </div>
              <p v-if="!paletteGroups.length" class="panel-empty">
                Không tìm thấy phần tử
              </p>
              <label class="palette-upload"
                >Ảnh / logo<input
                  type="file"
                  class="d-none"
                  accept="image/png,image/jpeg"
                  :disabled="busy"
                  @change="uploadImage"
              /></label>
            </template>
          </div>
          <div
            v-show="sidebarTab === 'outline'"
            class="layout-outline sidebar-content"
          >
            <fieldset :disabled="busy">
              <layout-block-tree
                :blocks="layout.body.children"
                :resolve-label="blockLabel"
                :selected-id="selected ? selected.id : ''"
                @select="selected = $event"
                @move="moveBlock"
              />
            </fieldset>
          </div>
        </aside>
        <main class="designer-document">
          <invoice-layout-canvas
            ref="canvas"
            :key="selectedTemplateId"
            :layout="layout"
            :template-id="selectedTemplateId"
            :base="base"
            :selected-id="selected ? selected.id : ''"
            :zoom="zoom"
            :rows="sampleRows"
            :disabled="busy"
            @select="selectBlock"
            @move="moveBlock"
            @insert="insertBlock"
            @text="changeText"
            @shortcut="handleShortcut"
          />
        </main>
        <aside
          v-show="showInspector"
          class="designer-inspector"
          aria-label="Thuộc tính phần tử"
        >
          <div class="inspector-heading">
            <span>Đang chọn</span
            ><strong>{{
              selected ? blockLabel(selected) : "Chưa chọn phần tử"
            }}</strong>
          </div>
          <div
            v-if="selected"
            class="panel-tabs"
            role="tablist"
            aria-label="Thuộc tính phần tử"
          >
            <button
              v-for="tab in inspectorTabs"
              :key="tab.value"
              type="button"
              role="tab"
              :class="{ active: inspectorTab === tab.value }"
              :aria-selected="inspectorTab === tab.value"
              @click="inspectorTab = tab.value"
            >
              {{ tab.text }}
            </button>
          </div>
          <fieldset :disabled="busy" class="inspector-content">
            <b-card
              title-tag="div"
              v-if="selected"
              v-show="inspectorTab === 'content'"
              title="Nội dung phần tử"
              class="mb-3"
            >
              <b-form-group v-if="selected.type === 'text'" label="Nội dung"
                ><b-form-textarea v-model="selected.text" rows="3"
              /></b-form-group>
              <template v-if="selected.type === 'field'">
                <b-form-group label="Nhãn"
                  ><b-form-input v-model="selected.label"
                /></b-form-group>
                <b-form-group label="Trường dữ liệu"
                  ><b-form-select
                    v-model="selected.fieldKey"
                    :options="scalarFields"
                /></b-form-group>
                <b-form-group label="Định dạng"
                  ><b-form-select
                    v-model="selected.formatter"
                    :options="formatters"
                /></b-form-group>
              </template>
              <b-form-group
                v-if="['table'].includes(selected.type)"
                label="Danh sách dữ liệu"
              >
                <b-form-select
                  v-model="selected.dataSource"
                  :options="repeatingDatasets"
                  @change="changeDataset"
                />
              </b-form-group>
              <template v-if="selected.columns">
                <details
                  v-for="(column, index) in selected.columns"
                  :key="column.id"
                  class="column-section"
                  :open="index === 0"
                >
                  <summary>
                    <span>{{ index + 1 }}. {{ column.label || "Cột mới" }}</span
                    ><small>{{ column.widthPercent }}%</small>
                  </summary>
                  <div class="column-settings">
                    <b-form-group label="Tên cột"
                      ><b-form-input
                        v-model="column.label"
                        placeholder="Tên cột"
                    /></b-form-group>
                    <b-form-group label="Trường dữ liệu"
                      ><b-form-select
                        v-model="column.fieldKey"
                        :options="
                          fieldsFor(selected.dataSource, column.fieldKey)
                        "
                    /></b-form-group>
                    <b-form-group label="Độ rộng (%)"
                      ><b-form-input
                        v-model.number="column.widthPercent"
                        type="number"
                        min="1"
                        max="100"
                    /></b-form-group>
                    <b-form-group label="Định dạng"
                      ><b-form-select
                        v-model="column.formatter"
                        :options="formatters"
                    /></b-form-group>
                    <b-form-group label="Căn chữ"
                      ><b-form-select
                        v-model="column.style.textAlign"
                        :options="alignments"
                    /></b-form-group>
                    <b-button
                      size="sm"
                      variant="outline-danger"
                      :disabled="selected.columns.length === 1"
                      @click="removeColumn(index)"
                      >Xóa cột</b-button
                    >
                  </div>
                </details>
                <b-button
                  size="sm"
                  variant="outline-primary"
                  :disabled="!fieldsFor(selected.dataSource).length"
                  @click="addColumn"
                  >Thêm cột</b-button
                ><small class="d-block text-muted my-2"
                  >Tổng độ rộng các cột phải bằng 100%.</small
                >
              </template>
            </b-card>
            <b-card
              title-tag="div"
              v-if="selected"
              v-show="inspectorTab === 'style'"
              title="Định dạng chữ"
              class="mb-3"
            >
              <div class="row">
                <div class="col-12">
                  <b-form-group label="Cỡ chữ"
                    ><b-form-input
                      v-model.number="selected.style.fontSizePt"
                      type="number"
                      min="7"
                      max="36"
                  /></b-form-group>
                </div>
                <div class="col-12">
                  <b-form-group label="Căn chữ"
                    ><b-form-select
                      v-model="selected.style.textAlign"
                      :options="alignments"
                  /></b-form-group>
                </div>
                <div class="col-12">
                  <b-form-group label="Nét chữ"
                    ><b-form-select
                      v-model.number="selected.style.fontWeight"
                      :options="[
                        { value: 400, text: 'Thường' },
                        { value: 700, text: 'Đậm' },
                      ]"
                  /></b-form-group>
                </div>
              </div>
            </b-card>
            <div v-if="!selected" class="inspector-empty">
              <span aria-hidden="true">↖</span>
              <strong>Chọn phần tử trên trang</strong>
            </div>
            <b-card
              title-tag="div"
              v-if="selected"
              v-show="inspectorTab === 'spacing'"
              title="Kích thước và khoảng cách"
              class="mb-3"
            >
              <b-form-group label="Khoảng đệm (mm)">
                <b-form-input
                  v-model.number="selected.style.paddingMm"
                  type="number"
                  min="0"
                  max="10"
                />
              </b-form-group>
              <b-form-group
                v-if="selected.children"
                label="Khoảng cách phần tử (mm)"
              >
                <b-form-input
                  v-model.number="selected.gapMm"
                  type="number"
                  min="0"
                  max="20"
                />
              </b-form-group>
              <template v-if="selected.type === 'image'">
                <b-form-group label="Rộng (mm)"
                  ><b-form-input
                    v-model.number="selected.widthMm"
                    type="number"
                    min="1"
                    max="270"
                /></b-form-group>
                <b-form-group label="Cao (mm)"
                  ><b-form-input
                    v-model.number="selected.heightMm"
                    type="number"
                    min="1"
                    max="150"
                /></b-form-group>
              </template>
              <b-form-group
                v-if="selected.type === 'spacer'"
                label="Chiều cao (mm)"
              >
                <b-form-input
                  v-model.number="selected.heightMm"
                  type="number"
                  min="1"
                  max="150"
                />
              </b-form-group>
              <b-form-group label="Viền (mm)"
                ><b-form-input
                  v-model.number="selected.style.borderWidthMm"
                  type="number"
                  min="0"
                  max="2"
                  step="0.1"
              /></b-form-group>
              <label
                >Màu viền
                <input
                  type="color"
                  :value="selectedStyle('borderColor', '#666666')"
                  @input="setStyle('borderColor', $event.target.value)"
              /></label>
            </b-card>
            <b-card
              title-tag="div"
              v-if="selected && selected.columns"
              v-show="inspectorTab === 'style'"
              title="Định dạng bảng"
              class="mb-3"
            >
              <div
                v-for="part in ['headerStyle', 'cellStyle']"
                :key="part"
                class="mb-3"
              >
                <strong class="d-block mb-2">{{
                  part === "headerStyle" ? "Hàng tiêu đề" : "Dòng dữ liệu"
                }}</strong>
                <div class="d-flex justify-content-between mb-2">
                  <label
                    >Màu nền
                    <input
                      type="color"
                      :value="selected[part].backgroundColor || '#ffffff'"
                      @input="
                        $set(
                          selected[part],
                          'backgroundColor',
                          $event.target.value
                        )
                      "
                  /></label>
                  <label
                    >Màu chữ
                    <input
                      type="color"
                      :value="selected[part].color || layout.theme.color"
                      @input="
                        $set(selected[part], 'color', $event.target.value)
                      "
                  /></label>
                </div>
                <b-form-group label="Cỡ chữ"
                  ><b-form-input
                    v-model.number="selected[part].fontSizePt"
                    type="number"
                    min="7"
                    max="36"
                /></b-form-group>
                <b-form-group label="Khoảng đệm ô (mm)"
                  ><b-form-input
                    v-model.number="selected[part].paddingMm"
                    type="number"
                    min="0"
                    max="10"
                    step="0.5"
                /></b-form-group>
              </div>
            </b-card>
          </fieldset>
        </aside>
      </div>
      <b-modal v-model="showPdf" title="Bản in PDF" size="xl" hide-footer>
        <b-alert v-if="dirty && jobId" show variant="warning"
          >Bản PDF thuộc bản nháp đã lưu trước đó. Đóng cửa sổ và chọn Bản in
          PDF để cập nhật.</b-alert
        >
        <pdf-artifact-viewer :job-id="jobId" />
      </b-modal>
    </template>
  </div>
</template>
<script>
import axios from "@/plugins/axios";
import LayoutBlockTree from "@/views/components/layout_block_tree.vue";
import PdfArtifactViewer from "@/views/components/pdf_artifact_viewer.vue";
import InvoiceLayoutCanvas from "@/views/components/invoice_layout_canvas.vue";
import {
  findBlock,
  blockId,
  copyBlock,
  placeBlock,
} from "@/utils/invoice_layout";

const requestKey = () =>
  window.crypto?.randomUUID
    ? window.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`;

const XML_GROUP_LABELS = Object.freeze({
  common: "Thông tin chung",
  related: "Hóa đơn liên quan",
  invoice: "Thông tin hóa đơn",
  seller: "Người bán",
  buyer: "Người mua",
  items: "Hàng hóa, dịch vụ",
  "items.special": "Hàng hóa đặc thù",
  taxGroups: "Tổng hợp thuế",
  totals: "Tổng tiền",
  fees: "Phí",
});

const XML_FIELD_LABEL_OVERRIDES = Object.freeze({
  "common.PBan": "Phiên bản",
});

export default {
  name: "InvoiceTemplateDesigner",
  components: { LayoutBlockTree, PdfArtifactViewer, InvoiceLayoutCanvas },
  data: () => ({
    selectedTemplateId: null,
    details: null,
    draft: null,
    layout: null,
    selected: null,
    fields: [],
    fieldGroups: [],
    datasets: [],
    xmlGroupFilter: "common",
    newBlockType: "field",
    busy: false,
    dirty: false,
    savedLayout: "",
    error: "",
    notice: "",
    jobId: null,
    generation: 0,
    showPdf: false,
    expanded: false,
    showSidebar: true,
    showInspector: true,
    sidebarTab: "elements",
    inspectorTab: "content",
    elementSearch: "",
    inspectorTabs: [
      { value: "content", text: "Nội dung" },
      { value: "style", text: "Định dạng" },
      { value: "spacing", text: "Bố cục" },
    ],
    blockIcons: {
      field: "{ }",
      text: "T",
      stack: "☰",
      columns: "▥",
      itemsTable: "▦",
      table: "▦",
      taxSummaryTable: "%",
      feesTable: "₫",
      totals: "Σ",
      signatureSummary: "✎",
      divider: "―",
      spacer: "↕",
      pageBreak: "↵",
    },
    activeTab: "home",

    zoom: 80,
    sampleRows: 3,
    history: [],
    historyIndex: -1,
    historyTimer: null,
    historyApplying: false,
    paletteDragActive: false,
    editorTabs: [
      { value: "home", text: "Trang đầu" },
      { value: "insert", text: "Chèn" },
      { value: "page", text: "Bố cục trang" },
      { value: "versions", text: "Lịch sử mẫu" },
    ],
    alignments: [
      { value: "left", text: "Căn trái" },
      { value: "center", text: "Căn giữa" },
      { value: "right", text: "Căn phải" },
    ],
    marginLabels: { top: "Trên", right: "Phải", bottom: "Dưới", left: "Trái" },
    formatters: [
      { value: "plainText", text: "Văn bản" },
      { value: "moneyByCurrency", text: "Tiền tệ" },
      { value: "decimal", text: "Số thập phân" },
      { value: "date", text: "Ngày tháng" },
      { value: "invoiceNumber", text: "Số hóa đơn" },
    ],
    blockTypes: [
      { value: "field", text: "Trường dữ liệu" },
      { value: "text", text: "Văn bản" },
      { value: "stack", text: "Nhóm dọc" },
      { value: "columns", text: "Nhóm cột" },
      { value: "itemsTable", text: "Bảng hàng hóa" },
      { value: "table", text: "Bảng dữ liệu" },
      { value: "taxSummaryTable", text: "Bảng thuế" },
      { value: "feesTable", text: "Bảng phí" },
      { value: "totals", text: "Nhóm tổng tiền" },
      { value: "signatureSummary", text: "Thông tin chữ ký" },
      { value: "divider", text: "Đường kẻ" },
      { value: "spacer", text: "Khoảng trống" },
      { value: "pageBreak", text: "Ngắt trang" },
    ],
  }),
  computed: {
    paletteGroups() {
      const normalize = (value) =>
        value
          .normalize("NFD")
          .replace(/[\u0300-\u036f]/g, "")
          .replace(/đ/g, "d")
          .toLowerCase();
      const query = normalize(this.elementSearch);
      const groups = [
        { label: "Nội dung", types: ["field", "text", "signatureSummary"] },
        {
          label: "Bảng và tổng tiền",
          types: [
            "itemsTable",
            "table",
            "taxSummaryTable",
            "feesTable",
            "totals",
          ],
        },
        {
          label: "Sắp xếp bố cục",
          types: ["stack", "columns", "divider", "spacer", "pageBreak"],
        },
      ];
      return groups
        .map((group) => ({
          label: group.label,
          items: this.blockTypes.filter(
            (type) =>
              group.types.includes(type.value) &&
              normalize(type.text).includes(query)
          ),
        }))
        .filter((group) => group.items.length);
    },
    xmlCatalogGroups() {
      const groups = new Map();
      this.fields
        .filter((field) => field.active !== false)
        .forEach((field) => {
          const code = field.group || "common";
          if (!groups.has(code)) groups.set(code, []);
          groups.get(code).push(field);
        });
      return Array.from(groups, ([code, items]) => ({
        code,
        label: this.xmlGroupLabel(code),
        items: items.map((field) => ({
          ...field,
          label: this.xmlFieldLabel(field),
        })),
      }));
    },
    xmlFieldGroups() {
      const normalize = (value) =>
        String(value || "")
          .normalize("NFD")
          .replace(/[\u0300-\u036f]/g, "")
          .replace(/đ/g, "d")
          .toLowerCase();
      const query = normalize(this.elementSearch);
      if (!query) return this.xmlCatalogGroups;
      return this.xmlCatalogGroups
        .map((group) => ({
          ...group,
          items: group.items.filter((field) =>
            normalize(`${field.label} ${field.key} ${field.group}`).includes(
              query
            )
          ),
        }))
        .filter((group) => group.items.length);
    },
    xmlGroupOptions() {
      return [
        { value: "", text: "Tất cả nhóm XML" },
        ...this.xmlCatalogGroups.map((group) => ({
          value: group.code,
          text: group.label,
        })),
      ];
    },
    visibleXmlFields() {
      const groups = this.xmlGroupFilter
        ? this.xmlFieldGroups.filter(
            (group) => group.code === this.xmlGroupFilter
          )
        : this.xmlFieldGroups;
      return groups.flatMap((group) => group.items);
    },

    canUndo() {
      return this.historyIndex > 0 || this.pendingHistory;
    },
    canRedo() {
      return (
        this.historyIndex < this.history.length - 1 && !this.pendingHistory
      );
    },
    pendingHistory() {
      return (
        !!this.layout &&
        JSON.stringify(this.layout) !== this.history[this.historyIndex]
      );
    },
    isAdmin() {
      return this.$route.path.startsWith("/administrator/");
    },
    base() {
      return this.isAdmin
        ? "/administrator/invoice-templates"
        : "/invoice-templates";
    },
    formId() {
      return this.$route.query.formId ? Number(this.$route.query.formId) : null;
    },
    saveStateLabel() {
      if (this.dirty) return "Chưa lưu";
      const latest = this.details?.versions?.[0];
      if (!latest) return "Đã lưu nháp";
      if (latest.contentHash && this.draft?.contentHash) {
        return latest.contentHash === this.draft.contentHash
          ? "Đã lưu"
          : "Đã lưu nháp";
      }
      return latest.sourceContent === this.draft?.sourceContent
        ? "Đã lưu"
        : "Đã lưu nháp";
    },
    saveStateVariant() {
      if (this.dirty) return "warning";
      return this.saveStateLabel === "Đã lưu" ? "success" : "info";
    },
    draftSourceLabel() {
      const source = this.details?.versions.find(
        (version) => version.id === this.draft?.baseVersionId
      );
      return source ? "Đang sửa từ mẫu đã lưu" : "";
    },
    scalarFields() {
      return this.fields
        .filter((f) => !f.repeated)
        .filter((f) => f.active !== false || this.selected?.fieldKey === f.key)
        .map((f) => ({ value: f.key, text: this.xmlFieldLabel(f) }));
    },
    repeatingDatasets() {
      return this.datasets
        .filter(
          (d) => d.cardinality === "MANY" && d.parent === "document" && d.active
        )
        .map((d) => ({ value: d.code, text: d.label }));
    },
  },
  watch: {
    expanded() {
      this.$nextTick(this.fitPage);
    },
    layout: {
      deep: true,
      handler() {
        if (this.layout && this.draft) {
          this.dirty = JSON.stringify(this.layout) !== this.savedLayout;
          if (!this.historyApplying) {
            clearTimeout(this.historyTimer);
            this.historyTimer = setTimeout(this.recordHistory, 350);
          }
        }
      },
    },
    selected(value) {
      if (value && !value.style) this.$set(value, "style", {});
    },
  },
  async created() {
    window.addEventListener("keydown", this.handleShortcut);
    try {
      const { data } = await axios.get(`${this.base}/catalog`);
      this.fields = data.fields;
      this.fieldGroups = data.groups || [];
      this.datasets = data.datasets || [];
      if (
        !this.xmlCatalogGroups.some(
          (group) => group.code === this.xmlGroupFilter
        )
      ) {
        this.xmlGroupFilter = this.xmlCatalogGroups[0]?.code || "";
      }
      if (this.formId) {
        const response = await axios.post(
          `${this.base}/from-form/${this.formId}`,
          {},
          { headers: { "Idempotency-Key": requestKey() } }
        );
        this.selectedTemplateId = response.data.template.id;
        await this.install(response.data);
      } else if (this.$route.query.templateId) {
        this.selectedTemplateId = Number(this.$route.query.templateId);
        await this.load();
      } else {
        this.$router.replace({
          name: this.isAdmin
            ? "admin-form-invoice-list"
            : "CustomerFormInvoiceList",
        });
      }
    } catch (e) {
      this.fail(e);
    }
  },
  beforeDestroy() {
    this.generation++;
    clearTimeout(this.historyTimer);
    window.removeEventListener("keydown", this.handleShortcut);
  },
  beforeRouteLeave(to, from, next) {
    next(!this.dirty || window.confirm("Bỏ các thay đổi chưa lưu?"));
  },
  methods: {
    xmlGroupLabel(code) {
      const configured = this.fieldGroups.find(
        (group) => group.code === code
      )?.label;
      if (configured && configured.trim().toLowerCase() !== code.toLowerCase()) {
        return configured;
      }
      return XML_GROUP_LABELS[code] || configured || code;
    },
    xmlFieldLabel(field) {
      if (!field) return "";
      if (XML_FIELD_LABEL_OVERRIDES[field.key]) {
        return XML_FIELD_LABEL_OVERRIDES[field.key];
      }
      return String(field.label || field.key || "")
        .replace(/\s*\([^)]*\)\s*$/u, "")
        .replace(/\s+/g, " ")
        .trim();
    },
    fitPage() {
      const canvas = this.$refs.canvas;
      const viewport = canvas?.$refs.viewport;
      if (viewport)
        this.zoom = Math.max(
          40,
          Math.min(
            100,
            Math.floor(((viewport.clientWidth - 48) / canvas.pageWidth) * 100)
          )
        );
    },
    togglePanel(name) {
      this[name] = !this[name];
      this.$nextTick(this.fitPage);
    },

    blockLabel(block) {
      return (
        block.label ||
        block.text ||
        this.xmlFieldLabel(
          this.fields.find((field) => field.key === block.fieldKey)
        ) ||
        this.blockTypes.find((type) => type.value === block.type)?.text ||
        block.type
      );
    },
    selectBlock(id) {
      this.selected = id
        ? findBlock(this.layout.body, id)?.block || null
        : null;
    },
    selectedStyle(key, fallback) {
      return this.selected?.style?.[key] ?? fallback;
    },
    setStyle(key, value) {
      if (!this.layout || this.busy) return;
      const block = this.selected || this.layout.body;
      this.selected = block;
      if (!block.style) this.$set(block, "style", {});
      this.$set(block.style, key, value);
    },
    resetHistory() {
      clearTimeout(this.historyTimer);
      this.history = this.layout ? [JSON.stringify(this.layout)] : [];
      this.historyIndex = this.history.length - 1;
    },
    recordHistory() {
      if (!this.layout || this.historyApplying) return;
      const snapshot = JSON.stringify(this.layout);
      if (snapshot === this.history[this.historyIndex]) return;
      this.history.splice(this.historyIndex + 1);
      this.history.push(snapshot);
      if (this.history.length > 80) this.history.shift();
      this.historyIndex = this.history.length - 1;
    },
    applyHistory(index) {
      clearTimeout(this.historyTimer);
      const selectedId = this.selected?.id;
      this.historyApplying = true;
      this.historyIndex = index;
      this.layout = JSON.parse(this.history[index]);
      this.selectBlock(selectedId);
      this.$nextTick(() => {
        this.historyApplying = false;
      });
    },
    undo() {
      if (this.busy) return;
      this.recordHistory();
      if (this.historyIndex > 0) this.applyHistory(this.historyIndex - 1);
    },
    redo() {
      if (!this.busy && this.canRedo) this.applyHistory(this.historyIndex + 1);
    },
    handleShortcut(event) {
      if (this.busy || this.showPdf || !this.layout) return;
      const input = event.target?.closest?.(
        "input, textarea, select, [contenteditable=true]"
      );
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        this.saveDraftOnly();
      } else if (!input && (event.ctrlKey || event.metaKey)) {
        const key = event.key.toLowerCase();
        if (key === "z" || key === "y") {
          event.preventDefault();
          if (key === "y" || event.shiftKey) this.redo();
          else this.undo();
        } else if (key === "d") {
          event.preventDefault();
          this.duplicateSelected();
        }
      } else if (
        !input &&
        (event.key === "Delete" || event.key === "Backspace")
      ) {
        event.preventDefault();
        this.deleteSelected();
      }
    },
    changeText({ id, text }) {
      if (this.busy || text.length > 5000) return;
      const found = findBlock(this.layout.body, id);
      if (found?.block.type === "text") found.block.text = text;
    },
    moveBlock({ id, targetId, placement }) {
      if (this.busy) return;
      const found = findBlock(this.layout.body, id);
      if (!found) return;
      this.recordHistory();
      if (
        placeBlock(this.layout.body, found.block, targetId, placement, true)
      ) {
        this.selected = found.block;
        this.recordHistory();
      } else
        this.error =
          "Không thể đặt phần tử ở vị trí này. Một nhóm cột chứa tối đa 6 phần tử.";
    },
    dragPalette(event, type) {
      if (this.busy) {
        event.preventDefault();
        return;
      }
      this.paletteDragActive = true;
      event.dataTransfer.setData("application/x-invoice-block-type", type);
      event.dataTransfer.effectAllowed = "copy";
    },
    dragXmlField(event, field) {
      if (this.busy || !field) {
        event.preventDefault();
        return;
      }
      this.paletteDragActive = true;
      event.dataTransfer.setData("application/x-invoice-block-type", "field");
      event.dataTransfer.setData("application/x-invoice-field-key", field.key);
      event.dataTransfer.effectAllowed = "copy";
    },
    finishPaletteDrag() {
      window.setTimeout(() => {
        this.paletteDragActive = false;
      }, 0);
    },
    insertBlock({ type, fieldKey, targetId, placement }) {
      const field = fieldKey
        ? this.fields.find((item) => item.key === fieldKey)
        : null;
      this.addBlock(
        type,
        { targetId, placement },
        fieldKey,
        this.xmlFieldLabel(field),
        true
      );
    },
    duplicateSelected() {
      if (!this.selected || this.busy) return;
      const found = findBlock(this.layout.body, this.selected.id);
      if (!found?.parent) return;
      if (
        found.parent.type === "columns" &&
        found.parent.children.length >= 6
      ) {
        this.error = "Một nhóm cột chứa tối đa 6 phần tử";
        return;
      }
      this.recordHistory();
      const copy = copyBlock(found.block);
      found.parent.children.splice(
        found.parent.children.indexOf(found.block) + 1,
        0,
        copy
      );
      this.selected = copy;
      this.recordHistory();
    },
    deleteSelected() {
      if (!this.selected || this.busy) return;
      const found = findBlock(this.layout.body, this.selected.id);
      if (!found?.parent) return;
      this.recordHistory();
      found.parent.children.splice(
        found.parent.children.indexOf(found.block),
        1
      );
      this.selected = null;
      this.recordHistory();
    },
    fail(e) {
      this.error =
        e.response?.data?.message ||
        e.message ||
        "Không thực hiện được thao tác";
    },
    fieldsFor(group, currentKey = null) {
      return this.fields
        .filter((f) => (f.rowScope || f.dataset || f.group) === group)
        .filter((f) => f.active !== false || f.key === currentKey)
        .map((f) => ({ value: f.key, text: this.xmlFieldLabel(f) }));
    },
    async syncCatalog() {
      const { data: catalog } = await axios.get(`${this.base}/catalog`);
      if (catalog.version === this.layout.fieldCatalogVersion) return;
      const { data: draft } = await axios.post(
        `${this.base}/${this.selectedTemplateId}/upgrade-catalog`,
        { expectedLockVersion: this.draft.lockVersion }
      );
      await this.install({ ...this.details, draft });
    },
    changeDataset() {
      if (!this.selected?.columns) return;
      const first = this.fieldsFor(this.selected.dataSource)[0];
      this.selected.columns = first
        ? [
            {
              id: blockId(),
              label: first.text,
              fieldKey: first.value,
              widthPercent: 100,
              formatter: "plainText",
              style: {},
            },
          ]
        : [];
    },
    async install(data) {
      this.details = data;
      this.draft = data.draft;
      if (data.draft) {
        try {
          this.layout = JSON.parse(data.draft.sourceContent);
        } catch (e) {
          this.layout = null;
          throw new Error(
            "Dữ liệu bố cục của mẫu không hợp lệ. Vui lòng tạo lại mẫu trong trình thiết kế."
          );
        }
      } else {
        this.layout = null;
      }
      const normalize = (block) => {
        if (!block.style) this.$set(block, "style", {});
        if (block.columns) {
          if (!block.headerStyle) this.$set(block, "headerStyle", {});
          if (!block.cellStyle) this.$set(block, "cellStyle", {});
          block.columns.forEach((column) => {
            if (!column.style) this.$set(column, "style", {});
          });
        }
        (block.children || []).forEach(normalize);
      };
      if (this.layout) {
        const response = await axios.get(`${this.base}/catalog`);
        if (response.data.version !== this.layout.fieldCatalogVersion) {
          const { data: draft } = await axios.post(
            `${this.base}/${this.selectedTemplateId}/upgrade-catalog`,
            { expectedLockVersion: this.draft.lockVersion }
          );
          this.draft = draft;
          this.layout = JSON.parse(draft.sourceContent);
        }
        this.fields = response.data.fields;
        this.fieldGroups = response.data.groups || [];
        this.datasets = response.data.datasets || [];
        normalize(this.layout.body);
      }
      this.savedLayout = this.layout ? JSON.stringify(this.layout) : "";
      this.selected = null;
      this.dirty = false;
      this.jobId = null;
      this.resetHistory();
      this.$nextTick(this.fitPage);
    },
    async load() {
      if (!this.selectedTemplateId) return;
      if (
        this.dirty &&
        !window.confirm("Bỏ thay đổi chưa lưu để mở mẫu khác?")
      ) {
        this.selectedTemplateId = this.details.template.id;
        return;
      }
      this.error = "";
      this.notice = "";
      this.busy = true;
      try {
        const { data } = await axios.get(
          `${this.base}/${this.selectedTemplateId}`
        );
        if (!data.draft) {
          const { data: editable } = await axios.post(
            `${this.base}/${this.selectedTemplateId}/ensure-draft`
          );
          await this.install(editable);
        } else {
          await this.install(data);
        }
      } catch (e) {
        this.fail(e);
      } finally {
        this.busy = false;
      }
    },
    async saveDraft() {
      if (!this.layout) return;
      const snapshot = JSON.parse(JSON.stringify(this.layout));
      const { data } = await axios.put(
        `${this.base}/${this.selectedTemplateId}/draft`,
        {
          expectedLockVersion: this.draft.lockVersion,
          layout: snapshot,
        }
      );
      this.draft = data;
      this.savedLayout = JSON.stringify(JSON.parse(data.sourceContent));
      this.dirty = JSON.stringify(this.layout) !== this.savedLayout;
    },
    async saveDraftOnly() {
      if (this.busy || !this.layout) return;
      this.busy = true;
      this.error = "";
      this.notice = "";
      try {
        if (this.dirty) await this.saveDraft();
        await this.syncCatalog();
        this.notice = "Đã lưu nháp";
      } catch (e) {
        this.fail(e);
      } finally {
        this.busy = false;
      }
    },
    async preview() {
      this.busy = true;
      this.error = "";
      try {
        if (this.dirty) await this.saveDraft();
        await this.syncCatalog();
        const { data } = await axios.post(
          `${this.base}/${this.selectedTemplateId}/preview`,
          {
            expectedLockVersion: this.draft.lockVersion,
          }
        );
        this.jobId = data.jobId || data.id;
        this.showPdf = true;
      } catch (e) {
        this.fail(e);
      } finally {
        this.busy = false;
      }
    },
    async saveVersion() {
      if (this.busy) return;
      this.busy = true;
      this.error = "";
      this.notice = "Đang kiểm tra bản in với 0, 1 và 60 dòng hàng hóa…";
      const generation = ++this.generation;
      try {
        if (this.dirty) await this.saveDraft();
        await this.syncCatalog();
        const revision = this.draft.lockVersion;
        const { data } = await axios.post(
          `${this.base}/${this.selectedTemplateId}/validate`,
          {
            expectedLockVersion: revision,
          }
        );
        const deadline = Date.now() + 180000;
        let run = data;
        while (run.state === "RUNNING" && Date.now() < deadline) {
          await new Promise((resolve) => setTimeout(resolve, 1000));
          if (generation !== this.generation) return;
          run = (await axios.get(`${this.base}/validation-runs/${data.id}`))
            .data;
        }
        if (run.state !== "PASSED") {
          const failed = JSON.parse(run.reportJson || "{}").fixtures?.find(
            (f) => f.errorCode
          );
          if (failed) {
            this.jobId = failed.jobId;
            this.showPdf = true;
          }
          throw new Error(
            "Bộ kiểm tra bản in chưa đạt. Xem lỗi ở phần bản in để chỉnh sửa hoặc thử lại."
          );
        }
        await axios.post(
          `${this.base}/${this.selectedTemplateId}/publish`,
          {
            expectedLockVersion: revision,
            validationReportId: data.id,
            formId: this.formId,
          },
          { headers: { "Idempotency-Key": requestKey() } }
        );
        this.dirty = false;
        this.notice = this.formId
          ? "Đã lưu và áp dụng mẫu hóa đơn"
          : "Đã lưu mẫu";
        await this.$router.push({
          name: this.isAdmin
            ? "admin-form-invoice-list"
            : "CustomerFormInvoiceList",
        });
      } catch (e) {
        this.fail(e);
        this.notice = "";
      } finally {
        this.busy = false;
      }
    },
    formatVersionDate(value) {
      if (!value) return "";
      const date = new Date(/(?:Z|[+-]\d{2}:\d{2})$/.test(value) ? value : `${value}Z`);
      if (Number.isNaN(date.getTime())) return "";
      return new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(date);
    },
    async deleteVersion(version) {
      if (this.busy || !window.confirm("Xóa mẫu đã lưu này?")) return;
      this.busy = true;
      this.error = "";
      this.notice = "";
      try {
        const { data } = await axios.delete(
          `${this.base}/${this.selectedTemplateId}/versions/${version.id}`
        );
        this.details.template = data.template;
        this.details.versions = data.versions;
        if (data.draft?.sourceContent === this.draft?.sourceContent) {
          this.draft = {
            ...this.draft,
            baseVersionId: data.draft.baseVersionId,
            lockVersion: data.draft.lockVersion,
          };
          this.details.draft = this.draft;
        }
        this.notice = "Đã xóa mẫu đã lưu";
      } catch (e) {
        this.fail(e);
      } finally {
        this.busy = false;
      }
    },
    async restore(version) {
      if (this.busy) return;
      if (
        this.dirty &&
        !window.confirm("Bỏ thay đổi chưa lưu để chỉnh sửa từ mẫu đã lưu?")
      ) return;
      this.busy = true;
      this.error = "";
      this.notice = "";
      try {
        const { data: draft } = await axios.post(
          `${this.base}/${this.selectedTemplateId}/draft-from-version`,
          {
            versionId: version.id,
            expectedLockVersion: this.draft.lockVersion,
          }
        );
        await this.install({ ...this.details, draft });
        this.activeTab = "home";
        this.notice = "Đang chỉnh sửa từ mẫu đã lưu";
      } catch (e) {
        this.fail(e);
      } finally {
        this.busy = false;
      }
    },
    async addBlock(
      type = this.newBlockType,
      position = null,
      fieldKey = null,
      fieldLabel = "",
      fromDrop = false
    ) {
      if (this.paletteDragActive && !fromDrop) return;
      if (
        this.busy ||
        !this.layout ||
        !this.blockTypes.some((item) => item.value === type)
      )
        return;
      const templateId = this.selectedTemplateId;
      const block = {
        id: blockId(),
        type,
        style: { fontSizePt: 10, fontWeight: 400, textAlign: "left" },
      };
      if (block.type === "field")
        Object.assign(block, {
          fieldKey: fieldKey || "seller.Ten",
          label:
            fieldLabel ||
            (fieldKey
              ? this.xmlFieldLabel(
                  this.fields.find((field) => field.key === fieldKey)
                )
              : ""),
          formatter: "plainText",
        });
      if (block.type === "text") block.text = "Nội dung";
      if (["stack", "columns", "totals"].includes(block.type)) {
        block.children = [];
        block.gapMm = 2;
      }
      if (block.type === "spacer") block.heightMm = 5;
      if (["table"].includes(block.type)) {
        const source = this.repeatingDatasets[0];
        if (!source) {
          this.error = "Danh mục chưa có danh sách lặp";
          return;
        }
        block.dataSource = source.value;
        if (block.type === "table") {
          const field = this.fieldsFor(source.value)[0];
          block.columns = field
            ? [
                {
                  id: blockId(),
                  label: field.text,
                  fieldKey: field.value,
                  widthPercent: 100,
                  formatter: "plainText",
                  style: {},
                },
              ]
            : [];
          block.repeatHeader = true;
          block.rowSplitPolicy = "splitIfOversized";
        }
      }
      if (block.type.endsWith("Table")) {
        block.dataSource = {
          itemsTable: "items",
          taxSummaryTable: "taxGroups",
          feesTable: "fees",
        }[block.type];
        block.repeatHeader = true;
        block.rowSplitPolicy = "splitIfOversized";
        const available = new Map(this.fields.map((field) => [field.key, field]));
        const limit = block.type === "taxSummaryTable" ? 3 : block.type === "itemsTable" ? 7 : 6;
        const fields = this.fieldsFor(block.dataSource).slice(0, limit).map((field) => ({
          field: available.get(field.value),
          label: field.text,
          formatter: block.type === "feesTable" ? "decimal" : "plainText",
        })).filter((item) => item.field);
        const width = fields.length ? Math.floor(100 / fields.length) : 100;
        block.columns = fields.map((item, index) => ({
          id: blockId(),
          fieldKey: item.field.key,
          label: item.label,
          widthPercent: index === fields.length - 1 ? 100 - width * index : width,
          formatter: item.formatter,
          style: {},
        }));
      }
      if (block.columns) {
        block.headerStyle = block.headerStyle || {};
        block.cellStyle = block.cellStyle || {};
        block.columns.forEach((column) => {
          if (!column.style) column.style = {};
        });
      }
      if (templateId !== this.selectedTemplateId || this.busy || !this.layout)
        return;
      this.recordHistory();
      let placed;
      if (position)
        placed = placeBlock(
          this.layout.body,
          block,
          position.targetId,
          position.placement
        );
      else if (this.selected?.children)
        placed = placeBlock(
          this.layout.body,
          block,
          this.selected.id,
          "inside"
        );
      else if (this.selected)
        placed = placeBlock(this.layout.body, block, this.selected.id, "after");
      else
        placed = placeBlock(
          this.layout.body,
          block,
          this.layout.body.id,
          "inside"
        );
      if (!placed) {
        this.error =
          "Không thể chèn vào vị trí này. Kiểm tra số phần tử trong nhóm.";
        return;
      }
      this.selected = block;
      this.recordHistory();
    },
    async addField(field) {
      if (!field || this.paletteDragActive) return;
      await this.addBlock("field");
      if (!this.selected || this.selected.type !== "field") return;
      this.$set(this.selected, "fieldKey", field.key);
      this.$set(this.selected, "label", this.xmlFieldLabel(field));
      this.recordHistory();
    },
    addColumn() {
      const field = this.fieldsFor(this.selected.dataSource)[0];
      if (!field || this.selected.columns.length >= 15) return;
      this.selected.columns.push({
        id: `c${Date.now()}`,
        label: field.text,
        fieldKey: field.value,
        widthPercent: 10,
        formatter: "plainText",
        style: {},
      });
      this.balanceColumns();
    },
    removeColumn(index) {
      this.selected.columns.splice(index, 1);
      this.balanceColumns();
    },
    balanceColumns() {
      const columns = this.selected.columns;
      const width = Math.floor(100 / columns.length);
      columns.forEach((column, index) => {
        column.widthPercent =
          index === columns.length - 1 ? 100 - width * index : width;
      });
    },
    async uploadImage(event) {
      const file = event.target.files[0];
      if (!file) return;
      const templateId = this.selectedTemplateId;
      try {
        const body = new FormData();
        body.append("file", file);
        body.append("kind", "IMAGE");
        const { data } = await axios.post(`${this.base}/assets`, body);
        if (templateId !== this.selectedTemplateId || !this.layout) return;
        const name = `image${data.id}`;
        this.$set(this.layout.assets, name, { assetId: data.id });
        this.layout.body.children.unshift({
          id: name,
          type: "image",
          asset: name,
          widthMm: 35,
          heightMm: 20,
        });
        this.selected = this.layout.body.children[0];
      } catch (e) {
        this.fail(e);
      } finally {
        event.target.value = "";
      }
    },
  },
};
</script>

<style scoped>
.template-designer {
  --designer-border: #dce2eb;
  --workspace-height: max(460px, calc(100dvh - 340px));
  color: #334155;
}
.designer-expanded {
  position: fixed;
  inset: 0;
  z-index: 1035;
  overflow: auto;
  background: #f4f6fa;
  --workspace-height: max(460px, calc(100dvh - 255px));
}
.designer-heading,
.document-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.designer-heading {
  margin-bottom: 14px;
}
.designer-heading h3 {
  font-size: 21px;
  font-weight: 600;
}
.designer-heading .btn {
  font-size: 13px;
}
.action-bar {
  gap: 8px;
}
.document-header {
  padding: 13px 16px;
  background: #fff;
  border: 1px solid var(--designer-border);
  border-radius: 8px 8px 0 0;
}
.document-header strong {
  font-size: 15px;
}
.document-header .badge {
  font-size: 11px;
  padding: 5px 8px;
}
.designer-ribbon {
  background: #fff;
  border: 1px solid var(--designer-border);
  border-top: 0;
}
.ribbon-tabs,
.panel-tabs {
  display: flex;
  border-bottom: 1px solid var(--designer-border);
}
.ribbon-tabs {
  padding: 0 12px;
  gap: 8px;
  align-items: stretch;
}
.ribbon-tabs > button,
.panel-tabs > button {
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #64748b;
  padding: 10px 12px;
  font-size: 13px;
  white-space: nowrap;
}
.ribbon-tabs > button.active,
.panel-tabs > button.active {
  color: #1d4ed8;
  border-bottom-color: #2563eb;
  font-weight: 600;
  background: #f8fbff;
}
.ribbon-tabs > button:hover,
.panel-tabs > button:hover {
  background: #f5f8fc;
  color: #1d4ed8;
}
.ribbon-tabs > button:focus-visible {
  outline: 1px solid #93c5fd;
  outline-offset: -3px;
}
.ribbon-content {
  margin: 0;
  padding: 0;
  border: 0;
}
.format-toolbar,
.insert-toolbar,
.page-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 14px;
  min-height: 60px;
}
.format-toolbar button,
.insert-toolbar button,
.workspace-toolbar button {
  color: #334155;
}
.format-toolbar button,
.insert-toolbar button,
.workspace-panels button,
.zoom-control button,
.workspace-toolbar button {
  border: 1px solid var(--designer-border);
  border-radius: 5px;
  background: #fff;
  padding: 6px 9px;
  font-size: 13px;
  min-height: 32px;
}
.format-toolbar button:hover,
.insert-toolbar button:hover,
.zoom-control button:hover {
  background: #eff6ff;
  border-color: #a5c4fa;
}
.format-toolbar button.active {
  color: #1d4ed8;
  background: #eaf2ff;
  border-color: #a5c4fa;
}
.template-designer button:disabled {
  opacity: 0.45;
  cursor: default;
}
.template-designer button:focus-visible,
.palette-upload:focus-within {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}
#app .template-designer .ribbon-tabs > button:focus-visible {
  outline: 1px solid #93c5fd;
  outline-offset: -3px;
}
.toolbar-group {
  display: flex;
  align-items: center;
  gap: 5px;
  padding-right: 12px;
  border-right: 1px solid var(--designer-border);
}
.toolbar-group select,
.font-size-input {
  height: 32px;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
  padding: 4px 7px;
  background: #fff;
  font-size: 13px;
  color: #334155;
}
.font-size-input {
  width: 56px;
}
.alignment-icon {
  display: block;
  width: 15px;
}
.alignment-icon i {
  display: block;
  height: 2px;
  margin: 3px 0;
  background: currentColor;
}
.alignment-icon i:nth-child(2) {
  width: 9px;
}
.alignment-icon.center i:nth-child(2) {
  margin-left: auto;
  margin-right: auto;
}
.alignment-icon.right i:nth-child(2) {
  margin-left: auto;
}
#app .template-designer .color-tool {
  display: inline-flex;
  position: relative;
  flex: 0 0 34px;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 32px;
  margin: 0;
  padding: 5px 7px;
  border: 1px solid var(--designer-border);
  border-radius: 5px;
  background: #fff;
  cursor: pointer;
}
#app .template-designer .color-tool:hover {
  background: #eff6ff;
  border-color: #a5c4fa;
}
#app .template-designer .color-tool:focus-within {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}
.color-tool-symbol {
  width: 18px;
  height: 21px;
  border-bottom: 4px solid;
  text-align: center;
  line-height: 17px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}
.color-tool-fill {
  height: 19px;
  border: 1px solid #cbd5e1;
  border-radius: 2px;
}
#app .template-designer .color-tool input[type="color"] {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  border: 0;
  opacity: 0;
  cursor: pointer;
}
.page-toolbar label {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 5px;
  width: 96px;
  line-height: 1.25;
  font-size: 12px;
  color: #64748b;
  margin: 0;
}
.page-toolbar {
  align-items: flex-end;
  row-gap: 12px;
}
.page-toolbar .page-orientation-setting {
  width: 110px;
}
.page-toolbar .page-margin-setting {
  width: 92px;
}
.page-toolbar .page-font-setting {
  width: 112px;
}
.page-toolbar .page-line-setting {
  width: 92px;
}
.page-toolbar .custom-select,
.page-toolbar .form-control {
  width: 100%;
}
.page-toolbar .btn {
  align-self: flex-end;
  white-space: nowrap;
}
.versions-panel {
  padding: 14px 16px 16px;
  background: #fbfcfe;
}
.versions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 34px;
}
.versions-header > div:first-child {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
}
.versions-header strong {
  color: #334155;
  font-size: 13px;
}
.versions-header span {
  color: #64748b;
  font-size: 12px;
}
.version-list {
  margin-top: 12px;
  border: 1px solid #e5eaf1;
  border-radius: 6px;
  background: #fff;
}
.version-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  padding: 9px 12px;
  border-top: 1px solid #eef2f6;
}
.version-row:first-child {
  border-top: 0;
}
.version-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
}
.version-info strong {
  color: #334155;
}
.version-latest {
  padding: 2px 8px;
  border-radius: 10px;
  color: #166534;
  background: #dcfce7;
}
.versions-empty {
  margin-top: 12px;
  padding: 16px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  color: #64748b;
  background: #fff;
  font-size: 12px;
  text-align: center;
}
.workspace-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  padding: 7px 12px;
  background: #f8fafc;
  border: 1px solid var(--designer-border);
  border-top: 0;
}
.workspace-panels {
  display: flex;
  gap: 5px;
}
.workspace-panels button.active {
  color: #1d4ed8;
  border-color: #bed3f6;
  background: #edf4ff;
}
.sample-rows {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}
.sample-rows .custom-select {
  width: 65px;
}
.zoom-control {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.zoom-control input {
  width: 85px;
}
.zoom-control span {
  min-width: 34px;
  font-variant-numeric: tabular-nums;
}
.zoom-control button {
  min-height: 28px;
  padding: 3px 8px;
  font-size: 12px;
}
.panel-tabs button.active {
  border-bottom-color: #2563eb;
  color: #1d4ed8;
  font-weight: 600;
}
.column-section summary span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.designer-workspace {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr) 290px;
  height: var(--workspace-height);
  border: 1px solid var(--designer-border);
  border-top: 0;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
}
.designer-workspace.sidebar-hidden {
  grid-template-columns: minmax(0, 1fr) 290px;
}
.designer-workspace.inspector-hidden {
  grid-template-columns: 210px minmax(0, 1fr);
}
.designer-workspace.sidebar-hidden.inspector-hidden {
  grid-template-columns: minmax(0, 1fr);
}
.designer-sidebar,
.designer-inspector {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: #fff;
}
.designer-sidebar {
  border-right: 1px solid var(--designer-border);
}
.designer-inspector {
  border-left: 1px solid var(--designer-border);
}
.designer-document {
  min-width: 0;
  min-height: 0;
}
.panel-tabs {
  border-bottom: 1px solid var(--designer-border);
  flex: 0 0 auto;
}
.panel-tabs button {
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  color: #64748b;
  flex: 1;
  padding: 11px 6px;
  font-size: 12px;
}
.designer-sidebar .panel-tabs {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 0.65fr) minmax(0, 1fr);
}
.designer-sidebar .panel-tabs button {
  padding: 11px 2px;
  font-size: 11px;
  white-space: nowrap;
}
.sidebar-content,
.inspector-content {
  overflow: auto;
  min-height: 0;
  padding: 14px;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}
.palette-section {
  margin-top: 20px;
}
.palette-group-title {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
  margin: 0 0 8px;
}
.block-palette {
  display: grid;
  gap: 4px;
}
.block-palette button {
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  padding: 7px 8px;
  border: 1px solid transparent;
  background: #fff;
  border-radius: 5px;
  color: #334155;
  font-size: 12px;
  cursor: grab;
}
.block-palette button:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}
.xml-field-palette {
  margin-top: 16px;
}
.xml-field-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.xml-field-heading small {
  margin-bottom: 8px;
  color: #94a3b8;
  font-size: 10px;
  white-space: nowrap;
}
.xml-group-select {
  margin-bottom: 6px;
}
.xml-field-list {
  gap: 4px;
}
.xml-field-list button {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  cursor: pointer;
}
.palette-icon.xml-field-icon {
  flex: 0 0 27px;
  font-size: 11px;
}
.xml-field-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}
.xml-field-label {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-height: 1.4;
  overflow-wrap: anywhere;
}
.xml-field-add {
  flex: 0 0 12px;
  color: #64748b;
  font-size: 16px;
  text-align: center;
}
.xml-field-empty {
  margin: 8px 0 0;
  color: #94a3b8;
  font-size: 11px;
  text-align: center;
}
.palette-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 27px;
  height: 27px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  color: #526681;
  font-size: 16px;
}
.palette-upload {
  display: block;
  margin: 18px 0 0;
  padding: 8px;
  text-align: center;
  font-size: 12px;
  border: 1px dashed #b8c5d6;
  border-radius: 5px;
  cursor: pointer;
}
.palette-upload:hover {
  background: #eff6ff;
  border-color: #93b4ee;
}
.layout-outline {
  font-size: 12px;
}
.panel-empty {
  color: #64748b;
  font-size: 12px;
  padding: 24px 0;
  text-align: center;
}
.inspector-heading {
  padding: 12px 14px;
  border-bottom: 1px solid #edf1f6;
}
.inspector-heading > span {
  display: block;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}
.inspector-heading strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
#app .template-designer .designer-inspector .card {
  border: 0;
  background: transparent;
  box-shadow: none;
}
#app .template-designer .designer-inspector ::v-deep .card-body {
  padding: 0;
}
.designer-inspector ::v-deep .card-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 18px;
}
#app .template-designer .designer-inspector ::v-deep label,
#app .template-designer .designer-inspector ::v-deep legend {
  color: #64748b;
  font-size: 12px !important;
  font-weight: 400;
  padding-bottom: 5px;
}
.designer-inspector ::v-deep .form-control,
.designer-inspector ::v-deep .custom-select {
  font-size: 13px;
  border-color: #d3dce8;
  border-radius: 5px;
}
.designer-inspector ::v-deep .form-group {
  margin-bottom: 14px;
}
.designer-inspector ::v-deep strong {
  font-size: 13px;
}
.inspector-empty {
  text-align: center;
  padding: 55px 8px;
  color: #64748b;
  font-size: 12px;
}
.inspector-empty span {
  display: block;
  font-size: 30px;
  margin-bottom: 14px;
  color: #a0aec0;
}
.inspector-empty strong {
  display: block;
  font-weight: 400;
}
.column-section {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  margin-bottom: 8px;
}
.column-section summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 10px;
  background: #f8fafc;
  cursor: pointer;
  font-size: 12px;
  list-style: none;
}
.column-section summary::before {
  content: "›";
  color: #64748b;
}
.column-section[open] summary::before {
  transform: rotate(90deg);
}
.column-section summary > span {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.column-section summary small {
  color: #64748b;
  white-space: nowrap;
}
.column-settings {
  padding: 12px 10px;
}
.template-designer label input[type="color"] {
  width: 36px;
  height: 25px;
  vertical-align: middle;
  border: 1px solid #dce2eb;
  border-radius: 4px;
  padding: 2px;
  background: #fff;
}
.workspace-toolbar .sample-rows {
  margin: 0;
}
.workspace-toolbar .zoom-control {
  margin-left: auto;
}
@media (max-width: 1280px) {
  .designer-workspace {
    grid-template-columns: 185px minmax(0, 1fr) 265px;
  }
  .designer-workspace.sidebar-hidden {
    grid-template-columns: minmax(0, 1fr) 265px;
  }
  .designer-workspace.inspector-hidden {
    grid-template-columns: 185px minmax(0, 1fr);
  }
  .designer-workspace.sidebar-hidden.inspector-hidden {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 960px) {
  .designer-sidebar .panel-tabs button {
    white-space: normal;
    line-height: 1.3;
  }
  .designer-heading,
  .document-header {
    flex-wrap: wrap;
  }
  .designer-workspace {
    grid-template-columns: 160px minmax(0, 1fr) 230px;
  }
  .designer-workspace.sidebar-hidden {
    grid-template-columns: minmax(0, 1fr) 230px;
  }
  .designer-workspace.inspector-hidden {
    grid-template-columns: 160px minmax(0, 1fr);
  }
  .designer-workspace.sidebar-hidden.inspector-hidden {
    grid-template-columns: minmax(0, 1fr);
  }
  .block-palette button {
    gap: 6px;
    padding-left: 0;
  }
  .sidebar-content,
  .inspector-content {
    padding: 10px;
  }
}
@media (max-width: 700px) {
  .designer-heading h3 {
    font-size: 18px;
  }
  .designer-workspace,
  .designer-workspace.sidebar-hidden,
  .designer-workspace.inspector-hidden {
    grid-template-columns: minmax(0, 1fr);
    height: auto;
  }
  .designer-sidebar {
    max-height: 320px;
    border-right: 0;
    border-bottom: 1px solid var(--designer-border);
  }
  .designer-inspector {
    max-height: 460px;
    border-left: 0;
    border-top: 1px solid var(--designer-border);
  }
  .designer-document {
    height: 560px;
  }
  .block-palette {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .ribbon-tabs {
    gap: 0;
    padding: 0 4px;
  }
  .ribbon-tabs > button {
    padding: 10px;
  }
  .page-toolbar {
    align-items: stretch;
  }
  .page-toolbar .page-setting,
  .page-toolbar .page-orientation-setting,
  .page-toolbar .page-margin-setting,
  .page-toolbar .page-font-setting,
  .page-toolbar .page-line-setting {
    width: calc(50% - 6px);
  }
  .toolbar-group {
    padding-right: 6px;
  }
  .versions-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .version-row {
    flex-wrap: wrap;
  }
}
</style>
