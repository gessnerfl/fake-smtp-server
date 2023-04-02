import { useRouteError } from "react-router-dom";

function ErrorPage() {
    const error = useRouteError();
    console.error(error);

    const getErrorMessage = function(error: any) {
        return error.statusText ? error.statusText : (error.message ? error.message :  "")
    }

    return (
        <div id="error-page">
            <h1>Oops!</h1>
            <p>Sorry, an unexpected error has occurred.</p>
            <p>
                <i>{getErrorMessage(error)}</i>
            </p>
        </div>
    );
}

export default ErrorPage;
