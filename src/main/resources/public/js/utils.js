// /**
//  * Send POST request
//  *
//  * @param {*} url
//  * @param {*} data
//  */
// async function post(url, data) {
//     try {
//         const res = await fetch(url, {
//             method: "POST",
//             body: JSON.stringify(data),
//         });

//         if (res.status === 200) {
//             return new Promise((resolve, reject) => {
//                 resolve(await res.text());
//             })
//         }

//         throw new Error();
//     } catch (e) {
//         console.log(e);
//         alert("Error with request!");
//     }
// }
