
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import OrderManager from "./components/orderManager"

import PayManager from "./components/payManager"

import DeliveryManager from "./components/deliveryManager"


import OderStatusView from "./components/oderStatusView"
export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/order',
                name: 'orderManager',
                component: orderManager
            },

            {
                path: '/pay',
                name: 'payManager',
                component: payManager
            },

            {
                path: '/delivery',
                name: 'deliveryManager',
                component: deliveryManager
            },


            {
                path: '/oderStatusView',
                name: 'oderStatusView',
                component: oderStatusView
            },


    ]
})
