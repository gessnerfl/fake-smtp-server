import * as React from 'react'
import '@testing-library/jest-dom'
import {render, screen} from '@testing-library/react'
import Shell from "./shell";

describe('Shell', () => {
    it('render shell component', () => {
        render(<Shell />);

        //Check for Navigation bar; Content not rendered as we check the element not in combination with router
        expect(screen.getByText("FakeSMTPServer")).toBeInTheDocument()
    })
})