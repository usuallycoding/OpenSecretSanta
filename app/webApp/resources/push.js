// What the web app needs from the browser to receive push notifications. The Kotlin side calls window.ossPush; nothing
// from Firebase is loaded until a person turns notifications on (or, later, opens the app with them on).
(function () {
    'use strict';

    var VERSION = '11.0.2';
    var BASE = 'https://www.gstatic.com/firebasejs/' + VERSION + '/';
    var WORKER = 'firebase-messaging-sw.js';

    var loading = null;
    var listening = false;

    // push needs https (or localhost), a service worker, push and notifications
    function supported() {
        return !!(window.isSecureContext && 'Notification' in window && 'serviceWorker' in navigator && 'PushManager' in window);
    }

    function permission() {
        return 'Notification' in window ? Notification.permission : 'denied';
    }

    // must be called while the click that asked for it is still fresh
    function requestPermission() {
        return Notification.requestPermission();
    }

    function registerWorker(config) {
        var query = new URLSearchParams({
            apiKey: config.apiKey,
            projectId: config.projectId,
            messagingSenderId: config.messagingSenderId,
            appId: config.appId
        });
        return navigator.serviceWorker.register(WORKER + '?' + query.toString()).then(function (registration) {
            return navigator.serviceWorker.ready.then(function () { return registration; });
        });
    }

    function load(config) {
        if (!loading) {
            loading = Promise.all([import(BASE + 'firebase-app.js'), import(BASE + 'firebase-messaging.js'), registerWorker(config)])
                .then(function (parts) {
                    var app = parts[0].initializeApp({
                        apiKey: config.apiKey,
                        projectId: config.projectId,
                        messagingSenderId: config.messagingSenderId,
                        appId: config.appId
                    });
                    return { api: parts[1], messaging: parts[1].getMessaging(app), registration: parts[2] };
                });
            loading.catch(function () { loading = null; });
        }
        return loading;
    }

    // a message that arrives while a page of the app is open comes to the page, not to the worker
    function show(registration, data) {
        if (!data || !data.body) return;
        registration.showNotification(data.title || 'Open Secret Santa', {
            body: data.body,
            tag: (data.category || 'message') + ':' + (data.groupId || ''),
            data: { groupId: data.groupId || '' }
        });
    }

    // the push token of this browser, or null when there is none to be had
    function token(configJson) {
        var config = JSON.parse(configJson);
        return load(config).then(function (firebase) {
            if (!listening) {
                listening = true;
                firebase.api.onMessage(firebase.messaging, function (payload) { show(firebase.registration, payload.data); });
            }
            return firebase.api.getToken(firebase.messaging, { vapidKey: config.vapidKey, serviceWorkerRegistration: firebase.registration })
                .then(function (value) { return value || null; });
        });
    }

    // throws this browser's token away, so that nothing more reaches it
    function remove(configJson) {
        var config = JSON.parse(configJson);
        return load(config).then(function (firebase) { return firebase.api.deleteToken(firebase.messaging); });
    }

    window.ossPush = {
        supported: supported,
        permission: permission,
        requestPermission: requestPermission,
        token: token,
        remove: remove
    };
})();
