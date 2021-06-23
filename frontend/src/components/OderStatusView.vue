<template>

<v-data-table
    :headers="headers"
    :items="oderStatusView"
    :items-per-page="5"
    class="elevation-1"
  ></v-data-table>

</template>

<script>
  const axios = require('axios').default;

  export default {
    name: 'OderStatusView',
    props: {
      value: Object,
      editMode: Boolean,
      isNew: Boolean
    },
    data: () => ({
        headers: [
            { text: "id", value: "id" },
            { text: "bookName", value: "bookName" },
            { text: "qty", value: "qty" },
            { text: "payId", value: "payId" },
            { text: "deliveryId", value: "deliveryId" },
            { text: "status", value: "status" },
            { text: "userName", value: "userName" },
        ],
        oderStatusView : [],
    }),
    async created() {
      var temp = await axios.get(axios.backend + '/oderstatusviews')

      this.oderStatusView = temp.data._embedded.oderstatusviews;

    },
    methods: {
    }
  }
</script>

