<template>
  <div class="p-3">
    <b-container>
      <h2>Danh sách hóa đơn5</h2>
      <b-table striped hover :items="invoices" :fields="fields"></b-table>
      <b-button variant="primary" class="mt-3" @click="reloadData">Tải lại</b-button>
    </b-container>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      invoices: [],
      fields: ['id', 'name', 'amount', 'date']
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    loadData() {
      axios.post('/invoices/init')
        .then(res => {
          this.invoices = res.data;
        })
        .catch(err => console.error(err));
    },
    reloadData() {
      this.loadData();
    }
  }
}
</script>

<style>
body {
  font-family: Arial, sans-serif;
}
</style>
