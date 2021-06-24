/**
 * Prefill the form with data based on the vaccine.
 *
 * @param {'AstraZeneca'|'Biontech'|'Johnson'|'Moderna'} value
 */
function prefillVaccineData(value) {
    /**
     * @type {HTMLFormElement}
     */
    const form = document.getElementById("vaccination-request-form");
    switch (value) {
        case "Biontech":
            form.elements["tg"].value = "840539006" // COVID19
            form.elements["vp"].value = "1119349007" // mRNA
            form.elements['mp'].value = "EU/1/20/1528" // Comirnaty
            form.elements['ma'].value = "ORG-100030215"
            form.elements['sd'].value = 2
            break;
        case "Johnson":
            form.elements["tg"].value = "840539006" // COVID19
            form.elements["vp"].value = "1119305005" // antigen vaccine
            form.elements['mp'].value = "EU/1/20/1525" // COVID-19 Vaccine Janssen
            form.elements['ma'].value = "ORG-100001417"
            form.elements['dn'].value = 1
            form.elements['sd'].value = 1
            break;
        case "Moderna":
            form.elements["tg"].value = "840539006" // COVID19
            form.elements["vp"].value = "1119349007" //mRNA
            form.elements['mp'].value = "EU/1/20/1507" // COVID-19 Vaccine Moderna
            form.elements['ma'].value = "ORG-100031184"
            form.elements['sd'].value = 2
            break;
        case "AstraZeneca":
            form.elements["tg"].value = "840539006" // COVID19
            form.elements["vp"].value = "1119305005" // antigen vaccine
            form.elements['mp'].value = "EU/1/21/1529" // Vaxzevria
            form.elements['ma'].value = "ORG-100001699"
            form.elements['sd'].value = 2
            break;
    }
}

/**
 * @param {HTMLInputElement} field field to set and focus.
 * @return {void} nothing
 */
function setCovid19ToField(field) {
    field.value = "840539006";
    field.focus()
}

async function sendVaccinationRequest() {
    const form = document.getElementById("vaccination-request-form");

    if (!form.reportValidity()) {
        return Promise.reject("invalid data");
        // TODO display error in frontend
    }

    const oVacinationRequest = {
        "nam": {
            "fn": form.elements["fn"].value,
            "gn": form.elements["gn"].value
        },
        "dob": form.elements["dob"].value,
        "v": [
            {
                "id": form.elements["id"].value,
                "tg": form.elements["tg"].value,
                "vp": form.elements["vp"].value,
                "mp": form.elements["mp"].value,
                "ma": form.elements["ma"].value,
                "dn": parseInt(form.elements["dn"].value),
                "sd": parseInt(form.elements["sd"].value),
                "dt": form.elements["dt"].value
            }
        ]
    };
    await sendRequest("../api/certify/v2/issue", oVacinationRequest)
}

function prefillVaccineParameters() {
    const form = document.getElementById("vaccination-request-form");

    // remove '#' from hash
    const params = new URLSearchParams(window.location.hash.substring(1));

    for (const name of ["fn", "gn", "dob", "id", "tg", "vp", "mp", "ma", "dn", "sd", "sd"]) {
        // setting the values to null will cause the validation to be triggered
        if (params.has(name)) {
            form.elements[name].value = params.get(name);
        }
    }
}

async function sendRecoveredRequest() {
    const form = document.getElementById("request-form");

    if (!form.reportValidity()) {
        return Promise.reject("form data is invalid");
        // TODO display error in frontend
    }

    const requestData = {
        "nam": {
            "fn": form.elements["fn"].value,
            "gn": form.elements["gn"].value
        },
        "dob": form.elements["dob"].value,
        "r": [
            {
                "id": form.elements["id"].value,
                "tg": form.elements["tg"].value,
                "fr": form.elements["fr"].value,
                "df": form.elements["df"].value,
                "du": form.elements["du"].value,
            }
        ]
    };
    await sendRequest("../api/certify/v2/recovered", requestData);
}

// Copy prefillParameter
function prefillRecoverParameters() {
    const form = document.getElementById("request-form");

    // remove '#' from hash
    const params = new URLSearchParams(window.location.hash.substring(1));

    for (const name of ["fn", "gn", "dob", "id", "tg", "fr", "df", "du"]) {
        // setting the values to null will cause the validation to be triggered
        if (params.has(name)) {
            form.elements[name].value = params.get(name);
        }
    }
}

function showError(message) {
    alert(message);
}

/**
 * @param {string} path the request path.
 * @param {{}} requestData
 * @return {Promise<void>} open dialog download dialog to download pdf file.
 */
async function sendRequest(path, requestData) {
    try {
        const response = await fetch(path, {
            method: "POST",
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(requestData)
        });

        const contentType = response.headers.get('Content-Type');

        if (response.status === 200 && contentType === 'application/pdf') {
            const buffer = await response.arrayBuffer();

            const blob = new Blob([buffer], {"type": "application/pdf"});
            window.location = URL.createObjectURL(blob);
        } else if (response.status >= 400 && contentType === 'application/json') {
            const error = await response.json();

            if (error['code'] && error['message']) {
                showError(error['code'] + ': ' + error['message']);
            } else {
                showError('Unknown error (unknown error response)');
            }
        } else {
            showError('Unknown error (unknown response code and content type)');
        }
    } catch (e) {
        console.error(e);
        showError(e.message);
    }
}
