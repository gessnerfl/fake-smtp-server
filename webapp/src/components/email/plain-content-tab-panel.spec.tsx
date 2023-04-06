import * as React from 'react'
import '@testing-library/jest-dom'
import {render, screen} from '@testing-library/react'
import {testEmail1} from "../../setupTests";
import {PlainContentTabPanel} from "./plain-content-tab-panel";

describe('PlainContentTabPanel', () => {
    it('render plain content tab panel component', () => {
        render(<PlainContentTabPanel activeContentType={"plain"} email={testEmail1} data={testEmail1.rawData} />);

        expect(screen.getByText(testEmail1.rawData.split("\n")[0])).toBeInTheDocument()
        expect(screen.getByText(testEmail1.rawData.split("\n")[1])).toBeInTheDocument()
    })
})