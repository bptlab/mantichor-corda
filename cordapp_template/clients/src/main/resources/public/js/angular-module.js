"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/example/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.getOrders = () => $http.get(apiBaseURL + "orders")
        .then((response) => demoApp.orders = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getMyOrders = () => $http.get(apiBaseURL + "my-orders")
        .then((response) => demoApp.myorders = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getOrders();
    demoApp.getMyOrders();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

        // Validates and sends Orders.
        modalInstance.create = function validateAndSendOrder() {
            if (modalInstance.form.menge <= 0 || modalInstance.form.preis <= 0) {
                modalInstance.formError = true;
            } else {
                modalInstance.formError = false;
                $uibModalInstance.close();

                let CREATE_ORDERS_PATH = apiBaseURL + "create-order"

                let createOrderData = $.param({
                    partyName: modalInstance.form.counterparty,
                    pizzaAmount : modalInstance.form.menge,
                    pizzaPrice : modalInstance.form.preis

                });

                let createOrderHeaders = {
                    headers : {
                        "Content-Type": "application/x-www-form-urlencoded"
                    }
                };

                // Create order  and handles success / fail responses.
                $http.post(CREATE_ORDERS_PATH, createOrderData, createOrderHeaders).then(
                    modalInstance.displayMessage,
                    modalInstance.displayMessage
                );
            }
        };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create Order modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Order.
    function invalidFormInput() {
        return isNaN(modalInstance.form.menge) || isNaN(modalInstance.form.preis) || (modalInstance.form.counterparty === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});