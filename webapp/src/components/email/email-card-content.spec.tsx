import {render, screen} from "@testing-library/react";
import {testEmail1} from "../../setupTests";
import * as React from "react";
import '@testing-library/jest-dom'
import {EmailCardContent} from "./email-card-content";

describe('EmailCardContent', () => {
    it('render email card content component with raw data only', () => {
        render(<EmailCardContent email={testEmail1}/>);

        //header in document
        expect(screen.getByText(testEmail1.fromAddress)).toBeInTheDocument()
        //content in document
        expect(screen.getByLabelText("Raw")).toBeInTheDocument()
    })
})