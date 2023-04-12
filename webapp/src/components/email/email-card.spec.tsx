import {render, screen} from "@testing-library/react";
import {testEmail1} from "../../setupTests";
import * as React from "react";
import '@testing-library/jest-dom'
import {EmailCard} from "./email-card";
import {MemoryRouter} from "react-router-dom";

describe('EmailCard', () => {
    it('render email card component with raw data only', () => {
        render(<MemoryRouter><EmailCard email={testEmail1}/></MemoryRouter>);

        //card header in document
        expect(screen.getByText("Email " + testEmail1.id)).toBeInTheDocument()
        expect(screen.getByRole("link", {name: "open"})).toBeInTheDocument()
        //card content (email header) in document
        expect(screen.getByText(testEmail1.fromAddress)).toBeInTheDocument()
    })
})