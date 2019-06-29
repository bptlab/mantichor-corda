"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/generatedside0f05a461e7046c68790a3249a304714/";
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
            modalInstance.formError = false;
            $uibModalInstance.close();

            let command = apiBaseURL;
            switch(parseFloat(modalInstance.form.stateEnum)) {
                case 0:
                    command += "createChoreographie";
                    break
                case 1:
                    command += "PizzaBestellen";
                    break
                case 2:
                    command += "PizzaLiefern";
                    break;
                case 3:
                    command += "GeldKassieren";
                    break;
            }

            let commandData = $.param({
                partyName: modalInstance.form.counterparty
            });

            let commandHeaders = {
                headers : {
                    "Content-Type": "application/x-www-form-urlencoded"
                }
            };

            // Create order  and handles success / fail responses.
            $http.post(command, commandData, commandHeaders).then(
                modalInstance.displayMessage,
                modalInstance.displayMessage
            );
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