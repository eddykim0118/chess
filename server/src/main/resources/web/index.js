let currentAuthToken = '';

function displayResponse(response, status) {
    const responseElement = document.getElementById('response');
    responseElement.textContent = `Status: ${status}\n${JSON.stringify(response, null, 2)}`;
}

async function makeRequest(method, endpoint, body = null, headers = {}) {
    try {
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                ...headers
            }
        };

        if (body) {
            options.body = JSON.stringify(body);
        }

        const response = await fetch(endpoint, options);
        const data = await response.json();

        displayResponse(data, response.status);
        return { data, status: response.status };
    } catch (error) {
        displayResponse({ error: error.message }, 'Error');
        return { error: error.message };
    }
}

async function clearDB() {
    await makeRequest('DELETE', '/db');
}

async function register() {
    const username = document.getElementById('regUsername').value;
    const password = document.getElementById('regPassword').value;
    const email = document.getElementById('regEmail').value;

    const result = await makeRequest('POST', '/user', {
        username: username,
        password: password,
        email: email
    });

    if (result.status === 200 && result.data.authToken) {
        currentAuthToken = result.data.authToken;
        document.getElementById('authToken').value = currentAuthToken;
    }
}

async function login() {
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    const result = await makeRequest('POST', '/session', {
        username: username,
        password: password
    });

    if (result.status === 200 && result.data.authToken) {
        currentAuthToken = result.data.authToken;
        document.getElementById('authToken').value = currentAuthToken;
    }
}

async function logout() {
    const authToken = document.getElementById('authToken').value || currentAuthToken;

    await makeRequest('DELETE', '/session', null, {
        'authorization': authToken
    });

    currentAuthToken = '';
    document.getElementById('authToken').value = '';
}

async function listGames() {
    const authToken = document.getElementById('authToken').value || currentAuthToken;

    await makeRequest('GET', '/game', null, {
        'authorization': authToken
    });
}

async function createGame() {
    const gameName = document.getElementById('gameName').value;
    const authToken = document.getElementById('authToken').value || currentAuthToken;

    await makeRequest('POST', '/game', {
        gameName: gameName
    }, {
        'authorization': authToken
    });
}

async function joinGame() {
    const gameID = parseInt(document.getElementById('gameID').value);
    const playerColor = document.getElementById('playerColor').value;
    const authToken = document.getElementById('authToken').value || currentAuthToken;

    await makeRequest('PUT', '/game', {
        playerColor: playerColor,
        gameID: gameID
    }, {
        'authorization': authToken
    });
}