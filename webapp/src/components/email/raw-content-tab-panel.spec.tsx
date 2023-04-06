import * as React from 'react'
import '@testing-library/jest-dom'
import {render, screen} from '@testing-library/react'
import {testEmail1} from "../../setupTests";
import {RawContentTabPanel} from "./raw-content-tab-panel";

describe('RawContentTabPanel', () => {
    it('render raw content tab panel component', () => {
        render(<RawContentTabPanel activeContentType={"raw"} email={testEmail1} data={testEmail1.rawData} />);

        expect(screen.getByText(testEmail1.rawData.replaceAll("\n", " "))).toBeInTheDocument()
    })
})